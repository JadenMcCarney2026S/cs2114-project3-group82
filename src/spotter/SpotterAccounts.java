package spotter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

final class SpotterAccounts
{
    static final int MIN_AGE = 18;
    static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    static final int MAX_DISPLAY_PHOTO_EDGE = 512;
    private static final int MAX_SOURCE_PHOTO_EDGE = 4096;
    private static final int MAX_ACCOUNTS = 1024;
    private static final int MAGIC = 0x53504131;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int HASH_ITERATIONS = 600_000;
    private static final int SESSION_SECONDS = 12 * 60 * 60;
    private static final int GLOBAL_ATTEMPTS_PER_MINUTE = 60;
    private static final int USER_FAILURES_PER_MINUTE = 8;

    record Session(String token, int userId)
    {
    }

    record Profile(UserProfile user, byte[] photo)
    {
        Profile
        {
            user = copy(user);
            photo = photo.clone();
        }

        @Override
        public UserProfile user()
        {
            return copy(user);
        }

        @Override
        public byte[] photo()
        {
            return photo.clone();
        }

        private static UserProfile copy(UserProfile source)
        {
            return new UserProfile(source.getUserId(), source.getName(), source.getAge(),
                source.getWorkoutExperience(), source.getGoal(), source.getGym(),
                source.getPreferredTime(), source.getBio());
        }
    }

    private record Account(String username, int id, byte[] salt, byte[] hash, Profile profile)
    {
    }

    private record ActiveSession(Session session, Instant expires)
    {
    }

    private final Path path;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final byte[] unknownAccountSalt = randomBytes(SALT_BYTES);
    private final Map<String, ActiveSession> sessions = new HashMap<>();
    private final Map<String, Integer> failedLogins = new HashMap<>();
    private Map<String, Account> accounts;
    private Instant rateWindowEnds = Instant.MIN;
    private int attempts;

    SpotterAccounts(Path path) throws IOException
    {
        this(path, Clock.systemUTC());
    }

    SpotterAccounts(Path path, Clock clock) throws IOException
    {
        this.path = path.toAbsolutePath();
        this.clock = clock;
        accounts = readAccounts();
    }

    synchronized Session signUp(String username, String password) throws IOException
    {
        String name = normalizeUsername(username);
        validatePassword(password);
        countAttempt();
        if (accounts.containsKey(name))
        {
            throw new IllegalArgumentException("That username is already taken.");
        }
        if (accounts.size() >= MAX_ACCOUNTS)
        {
            throw new IllegalStateException("This host has reached its account limit.");
        }
        int id = accounts.values().stream().mapToInt(Account::id).max().orElse(0) + 1;
        byte[] salt = randomBytes(SALT_BYTES);
        save(new Account(name, id, salt, hash(password, salt), null));
        return createSession(id);
    }

    synchronized Session login(String username, String password)
    {
        String name = normalizeUsername(username);
        validatePassword(password);
        countAttempt();
        int failures = failedLogins.getOrDefault(name, 0);
        if (failures >= USER_FAILURES_PER_MINUTE)
        {
            throw new IllegalStateException("Too many attempts. Try again in a minute.");
        }
        Account account = accounts.get(name);
        byte[] supplied = hash(password, account == null ? unknownAccountSalt : account.salt());
        boolean matches = MessageDigest.isEqual(supplied,
            account == null ? new byte[HASH_BYTES] : account.hash());
        Arrays.fill(supplied, (byte)0);
        if (account == null || !matches)
        {
            failedLogins.put(name, failures + 1);
            throw new SecurityException("Invalid username or password.");
        }
        failedLogins.remove(name);
        return createSession(account.id());
    }

    synchronized Session authenticate(String token)
    {
        ActiveSession active = sessions.get(token);
        if (active == null)
        {
            return null;
        }
        if (!clock.instant().isBefore(active.expires()))
        {
            sessions.remove(token);
            return null;
        }
        return active.session();
    }

    synchronized void logout(String token)
    {
        sessions.remove(token);
    }

    synchronized List<Profile> profiles()
    {
        return accounts.values().stream().map(Account::profile)
            .filter(java.util.Objects::nonNull).toList();
    }

    synchronized Profile profile(int id)
    {
        Account account = findAccount(id);
        return account == null ? null : account.profile();
    }

    synchronized Profile complete(int id, String name, int age, String interest, String bio,
        byte[] photo) throws IOException
    {
        Account account = findAccount(id);
        if (account == null || account.profile() != null)
        {
            throw new IllegalArgumentException("Profile setup is unavailable.");
        }
        UserProfile user = validateProfile(id, name, age, interest, bio);
        Profile profile = new Profile(user, normalizePhoto(photo));
        save(new Account(account.username(), id, account.salt(), account.hash(), profile));
        return profile;
    }

    private Account findAccount(int id)
    {
        return accounts.values().stream().filter(account -> account.id() == id)
            .findFirst().orElse(null);
    }

    private Session createSession(int id)
    {
        Instant now = clock.instant();
        sessions.values().removeIf(active ->
            active.session().userId() == id || !now.isBefore(active.expires()));
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
        Session session = new Session(token, id);
        sessions.put(token, new ActiveSession(session, now.plusSeconds(SESSION_SECONDS)));
        return session;
    }

    private void countAttempt()
    {
        Instant now = clock.instant();
        if (!now.isBefore(rateWindowEnds))
        {
            rateWindowEnds = now.plusSeconds(60);
            attempts = 0;
            failedLogins.clear();
        }
        if (attempts >= GLOBAL_ATTEMPTS_PER_MINUTE)
        {
            throw new IllegalStateException("Too many attempts. Try again in a minute.");
        }
        attempts++;
    }

    private static String normalizeUsername(String username)
    {
        if (username == null || !username.strip().matches("[A-Za-z0-9_]{3,32}"))
        {
            throw new IllegalArgumentException("Use 3–32 letters, digits, or underscores for the username.");
        }
        return username.strip().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password)
    {
        if (password == null || password.length() < 12 || password.length() > 128)
        {
            throw new IllegalArgumentException("Use 12–128 characters for the password.");
        }
    }

    private static UserProfile validateProfile(int id, String name, int age, String interest,
        String bio)
    {
        if (age < MIN_AGE)
        {
            throw new IllegalArgumentException("You must be at least 18 to create a profile.");
        }
        if (name == null || name.isBlank() || name.length() > 80
            || interest == null || interest.isBlank() || interest.length() > 80
            || bio == null || bio.length() > 500)
        {
            throw new IllegalArgumentException("Enter a name and workout interest of up to 80 characters, and a bio of up to 500.");
        }
        return new UserProfile(id, name.strip(), age,
            Map.of(interest.strip().toLowerCase(Locale.ROOT), "Unspecified"), "", "", "", bio);
    }

    private byte[] randomBytes(int size)
    {
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }

    private static byte[] hash(String password, byte[] salt)
    {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, HASH_ITERATIONS, HASH_BYTES * 8);
        Arrays.fill(characters, '\0');
        try
        {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        }
        catch (GeneralSecurityException failure)
        {
            throw new IllegalStateException("Password hashing is unavailable.", failure);
        }
        finally
        {
            spec.clearPassword();
        }
    }

    private static byte[] normalizePhoto(byte[] photo)
    {
        if (photo == null || photo.length == 0 || photo.length > MAX_PHOTO_BYTES)
        {
            throw new IllegalArgumentException("Choose a JPEG or PNG photo up to 2 MiB.");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(photo)))
        {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
            {
                throw new IllegalArgumentException("Choose a readable JPEG or PNG photo.");
            }
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png"))
                {
                    throw new IllegalArgumentException("Choose a JPEG or PNG photo.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int edge = Math.max(width, height);
                if (width < 1 || height < 1 || edge > MAX_SOURCE_PHOTO_EDGE)
                {
                    throw new IllegalArgumentException("Photo dimensions must be at most 4096 pixels per side.");
                }
                return resizePhoto(reader.read(0), width, height, edge);
            }
            finally
            {
                reader.dispose();
            }
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException("Choose a readable JPEG or PNG photo.", failure);
        }
    }

    private static byte[] resizePhoto(BufferedImage source, int width, int height, int edge)
        throws IOException
    {
        double ratio = Math.min(1.0, (double)MAX_DISPLAY_PHOTO_EDGE / edge);
        int scaledWidth = Math.max(1, (int)Math.round(width * ratio));
        int scaledHeight = Math.max(1, (int)Math.round(height * ratio));
        BufferedImage image = new BufferedImage(scaledWidth, scaledHeight,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, scaledWidth, scaledHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        }
        finally
        {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var imageOutput = new MemoryCacheImageOutputStream(output))
        {
            if (!ImageIO.write(image, "jpeg", imageOutput))
            {
                throw new IOException("JPEG encoder unavailable.");
            }
        }
        return output.toByteArray();
    }

    private Map<String, Account> readAccounts() throws IOException
    {
        Map<String, Account> result = new LinkedHashMap<>();
        if (Files.notExists(path))
        {
            return result;
        }
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path))))
        {
            if (input.readInt() != MAGIC)
            {
                throw new IOException("Unsupported account store.");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_ACCOUNTS)
            {
                throw new IOException("Invalid account store.");
            }
            var ids = new java.util.HashSet<Integer>();
            for (int i = 0; i < count; i++)
            {
                Account account = readAccount(input);
                if (!ids.add(account.id()) || result.putIfAbsent(account.username(), account) != null)
                {
                    throw new IOException("Duplicate account in storage.");
                }
            }
            if (input.read() != -1)
            {
                throw new IOException("Invalid account store.");
            }
        }
        catch (IllegalArgumentException invalid)
        {
            throw new IOException("Invalid account store.", invalid);
        }
        return result;
    }

    private static Account readAccount(DataInputStream input) throws IOException
    {
        String username = normalizeUsername(input.readUTF());
        int id = input.readInt();
        if (id < 1 || id > MAX_ACCOUNTS)
        {
            throw new IOException("Invalid account ID.");
        }
        byte[] salt = readBytes(input, SALT_BYTES);
        byte[] digest = readBytes(input, HASH_BYTES);
        Profile profile = null;
        if (input.readBoolean())
        {
            UserProfile user = validateProfile(id, input.readUTF(), input.readInt(),
                input.readUTF(), input.readUTF());
            int length = input.readInt();
            if (length < 1 || length > MAX_PHOTO_BYTES)
            {
                throw new IOException("Invalid stored photo size.");
            }
            profile = new Profile(user, readBytes(input, length));
        }
        return new Account(username, id, salt, digest, profile);
    }

    private static byte[] readBytes(DataInputStream input, int length) throws IOException
    {
        byte[] result = input.readNBytes(length);
        if (result.length != length)
        {
            throw new EOFException("Truncated account store.");
        }
        return result;
    }

    private void save(Account account) throws IOException
    {
        Map<String, Account> updated = new LinkedHashMap<>(accounts);
        updated.put(account.username(), account);
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".spotter-accounts-", ".tmp");
        try
        {
            writeAccounts(temporary, updated);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
            accounts = updated;
        }
        catch (IOException failure)
        {
            try
            {
                Files.deleteIfExists(temporary);
            }
            catch (IOException cleanup)
            {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    private static void writeAccounts(Path temporary, Map<String, Account> updated)
        throws IOException
    {
        // shortcut: each save rewrites the bounded demo store; use a database for larger rosters.
        try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE);
            var output = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel))))
        {
            output.writeInt(MAGIC);
            output.writeInt(updated.size());
            for (Account item : updated.values())
            {
                writeAccount(output, item);
            }
            output.flush();
            channel.force(true);
        }
    }

    private static void writeAccount(DataOutputStream output, Account account) throws IOException
    {
        output.writeUTF(account.username());
        output.writeInt(account.id());
        output.write(account.salt());
        output.write(account.hash());
        Profile profile = account.profile();
        output.writeBoolean(profile != null);
        if (profile != null)
        {
            UserProfile user = profile.user;
            output.writeUTF(user.getName());
            output.writeInt(user.getAge());
            output.writeUTF(user.getWorkoutExperience().keySet().iterator().next());
            output.writeUTF(user.getBio());
            output.writeInt(profile.photo.length);
            output.write(profile.photo);
        }
    }
}

package spotter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.SSLContext;

final class SpotterLanClient
{
    record Snapshot(boolean accepted, int candidate, List<Integer> matches, String history,
        int start, int next)
    {
    }

    record Account(boolean profileComplete)
    {
    }

    record Profile(String name, int age, String interest, String bio, byte[] photo)
    {
    }

    static final class SessionExpiredException extends IOException
    {
        SessionExpiredException()
        {
            super("Your session expired. Log in again.");
        }
    }

    @FunctionalInterface
    private interface AccountBody
    {
        void write(DataOutputStream data) throws IOException;
    }

    private final HttpClient http;
    private final URI address;
    private volatile String token;

    SpotterLanClient(URI address)
    {
        this(address, null);
    }

    SpotterLanClient(URI address, SSLContext tls)
    {
        if (!"https".equals(address.getScheme()) || address.getHost() == null
            || address.getUserInfo() != null || !"/chat".equals(address.getPath())
            || address.getQuery() != null || address.getFragment() != null)
        {
            throw new IllegalArgumentException("Enter an HTTPS server URL ending in /chat.");
        }
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3));
        if (tls != null)
        {
            builder.sslContext(tls);
        }
        http = builder.build();
        this.address = address;
    }

    Account signUp(String username, String password) throws IOException, InterruptedException
    {
        return authenticate("signup", username, password);
    }

    Account login(String username, String password) throws IOException, InterruptedException
    {
        return authenticate("login", username, password);
    }

    private Account authenticate(String action, String username, String password)
        throws IOException, InterruptedException
    {
        if (username.length() > 64 || password.length() > 256)
        {
            throw new IOException("Username or password is too long.");
        }
        return accountResult(accountRequest(action, false, data -> {
            data.writeUTF(username);
            data.writeUTF(password);
        }));
    }

    Account completeProfile(String name, int age, String interest, String bio, byte[] photo)
        throws IOException, InterruptedException
    {
        return accountResult(accountRequest("complete", true, data -> {
            data.writeUTF(name);
            data.writeInt(age);
            data.writeUTF(interest);
            data.writeUTF(bio);
            data.writeInt(photo.length);
            data.write(photo);
        }));
    }

    Profile profile(int id) throws IOException, InterruptedException
    {
        try (DataInputStream data = accountRequest("profile", true, body -> body.writeInt(id)))
        {
            String name = data.readUTF();
            int age = data.readInt();
            String interest = data.readUTF();
            String bio = data.readUTF();
            int size = data.readInt();
            if (size <= 0 || size > SpotterAccounts.MAX_PHOTO_BYTES)
            {
                throw new IOException("Invalid profile photo.");
            }
            byte[] photo = data.readNBytes(size);
            if (photo.length != size || data.read() != -1)
            {
                throw new IOException("Incomplete profile response.");
            }
            return new Profile(name, age, interest, bio, photo);
        }
    }

    void logout() throws IOException, InterruptedException
    {
        try (DataInputStream response = accountRequest("logout", true, data -> { }))
        {
            if (response.read() != -1)
            {
                throw new IOException("Invalid logout response.");
            }
        }
        finally
        {
            token = null;
        }
    }

    private Account accountResult(DataInputStream data) throws IOException
    {
        try (data)
        {
            String session = data.readUTF();
            boolean complete = data.readBoolean();
            if (session.isBlank() || data.read() != -1)
            {
                throw new IOException("Invalid account response.");
            }
            token = session;
            return new Account(complete);
        }
    }

    private DataInputStream accountRequest(String action, boolean needsSession, AccountBody fields)
        throws IOException, InterruptedException
    {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(body))
        {
            data.writeUTF(action);
            fields.write(data);
        }
        HttpRequest request = requestBuilder(address.resolve("/account"), needsSession)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 401 && needsSession)
        {
            throw new SessionExpiredException();
        }
        if (response.statusCode() != 200)
        {
            throw new IOException(new String(response.body(), StandardCharsets.UTF_8));
        }
        return new DataInputStream(new ByteArrayInputStream(response.body()));
    }

    private HttpRequest.Builder requestBuilder(URI uri, boolean needsSession) throws IOException
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8));
        if (needsSession)
        {
            String session = token;
            if (session == null)
            {
                throw new SessionExpiredException();
            }
            builder.header("Authorization", "Bearer " + session);
        }
        return builder;
    }

    Snapshot request(String action, int peer, String message)
        throws IOException, InterruptedException
    {
        return request(action, peer, message,
            "send".equals(action) ? UUID.randomUUID().toString() : "", 0);
    }

    Snapshot request(String action, int peer, String message, String requestId, int after)
        throws IOException, InterruptedException
    {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(body))
        {
            data.writeUTF(action);
            data.writeInt(peer);
            data.writeUTF(message);
            data.writeUTF(requestId);
            data.writeInt(after);
        }
        HttpRequest request = requestBuilder(address, true)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401)
        {
            throw new SessionExpiredException();
        }
        if (response.statusCode() != 200)
        {
            throw new IOException(response.body());
        }
        String[] parts = response.body().split("\n", 6);
        try
        {
            List<Integer> matches = parts[2].isEmpty() ? List.of()
                : Arrays.stream(parts[2].split(",")).map(Integer::parseInt).toList();
            return new Snapshot(Boolean.parseBoolean(parts[0]), Integer.parseInt(parts[1]),
                matches, parts[5], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        }
        catch (NumberFormatException | IndexOutOfBoundsException malformed)
        {
            throw new IOException("Invalid Spotter server response.", malformed);
        }
    }
}

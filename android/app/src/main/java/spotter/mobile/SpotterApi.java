package spotter.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/** Android-compatible client for the existing Spotter HTTPS endpoints. */
final class SpotterApi
{
    static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;

    record Profile(String name, int age, String interest, String bio, byte[] photo)
    {
    }

    record Snapshot(boolean accepted, int candidate, List<Integer> matches, String history,
        int start, int next)
    {
    }

    static final class SessionExpired extends IOException
    {
        private static final long serialVersionUID = 1L;

        SessionExpired()
        {
            super("Your session expired. Log in again.");
        }
    }

    private interface Payload
    {
        void write(DataOutputStream data) throws IOException;
    }

    private final URI address;
    private final SSLSocketFactory tls;
    private volatile String token;

    SpotterApi(String baseUrl, InputStream certificate) throws Exception
    {
        address = URI.create(baseUrl);
        if (!"https".equals(address.getScheme()) || address.getHost() == null
            || !"/chat".equals(address.getRawPath()) || address.getRawUserInfo() != null
            || address.getRawQuery() != null || address.getRawFragment() != null)
        {
            throw new IllegalArgumentException("Enter an HTTPS server URL ending in /chat.");
        }

        KeyStore trustedServer = KeyStore.getInstance(KeyStore.getDefaultType());
        trustedServer.load(null, null);
        trustedServer.setCertificateEntry("spotter", CertificateFactory.getInstance("X.509")
            .generateCertificate(certificate));
        TrustManagerFactory trust = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustedServer);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        tls = context.getSocketFactory();
    }

    boolean login(String username, String password) throws IOException
    {
        return authenticate("login", username, password);
    }

    boolean signUp(String username, String password) throws IOException
    {
        return authenticate("signup", username, password);
    }

    private boolean authenticate(String action, String username, String password) throws IOException
    {
        if (username.length() > 64 || password.length() > 256)
        {
            throw new IOException("Username or password is too long.");
        }
        return accountResult(account(action, false, data -> {
            data.writeUTF(username);
            data.writeUTF(password);
        }, 512));
    }

    void complete(String name, int age, String interest, String bio, byte[] photo) throws IOException
    {
        if (name.length() > 80 || interest.length() > 80 || bio.length() > 500
            || photo.length == 0 || photo.length > MAX_PHOTO_BYTES)
        {
            throw new IOException("Check profile lengths and use a photo of at most 2 MiB.");
        }
        if (!accountResult(account("complete", true, data -> {
            data.writeUTF(name);
            data.writeInt(age);
            data.writeUTF(interest);
            data.writeUTF(bio);
            data.writeInt(photo.length);
            data.write(photo);
        }, 512)))
        {
            throw new IOException("The server did not complete your profile.");
        }
    }

    Profile profile(int id) throws IOException
    {
        try (DataInputStream data = account("profile", true, body -> body.writeInt(id),
            MAX_PHOTO_BYTES + 4096))
        {
            String name = data.readUTF();
            int age = data.readInt();
            String interest = data.readUTF();
            String bio = data.readUTF();
            int size = data.readInt();
            if (size <= 0 || size > MAX_PHOTO_BYTES || size != data.available())
            {
                throw new IOException("Invalid profile photo.");
            }
            byte[] photo = new byte[size];
            data.readFully(photo);
            return new Profile(name, age, interest, bio, photo);
        }
    }

    Snapshot request(String action, int peer, String text, String sendId, int after)
        throws IOException
    {
        if (text.length() > 500 || sendId.length() > 36)
        {
            throw new IOException("Messages can contain up to 500 characters.");
        }
        byte[] body = post(address, true, data -> {
            data.writeUTF(action);
            data.writeInt(peer);
            data.writeUTF(text);
            data.writeUTF(sendId);
            data.writeInt(after);
        }, 256 * 1024);
        String[] parts = new String(body, StandardCharsets.UTF_8).split("\n", 6);
        if (parts.length != 6 || !(parts[0].equals("true") || parts[0].equals("false")))
        {
            throw new IOException("Invalid Spotter response.");
        }
        try
        {
            List<Integer> matches = new ArrayList<>();
            if (!parts[2].isEmpty())
            {
                for (String match : parts[2].split(","))
                {
                    matches.add(Integer.parseInt(match));
                }
            }
            int candidate = Integer.parseInt(parts[1]);
            int start = Integer.parseInt(parts[3]);
            int next = Integer.parseInt(parts[4]);
            if (candidate < 0 || start < 0 || next < start)
            {
                throw new IOException("Invalid Spotter response.");
            }
            return new Snapshot(Boolean.parseBoolean(parts[0]), candidate,
                Collections.unmodifiableList(matches), parts[5], start, next);
        }
        catch (NumberFormatException failure)
        {
            throw new IOException("Invalid Spotter response.", failure);
        }
    }

    void logout() throws IOException
    {
        try (DataInputStream response = account("logout", true, data -> { }, 512))
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

    private boolean accountResult(DataInputStream data) throws IOException
    {
        try (DataInputStream response = data)
        {
            String session = response.readUTF();
            boolean complete = response.readBoolean();
            if (!session.matches("[A-Za-z0-9_-]{43}") || response.read() != -1)
            {
                throw new IOException("Invalid account response.");
            }
            token = session;
            return complete;
        }
    }

    private DataInputStream account(String action, boolean needsSession, Payload fields, int limit)
        throws IOException
    {
        return new DataInputStream(new ByteArrayInputStream(post(address.resolve("/account"),
            needsSession, data -> {
                data.writeUTF(action);
                fields.write(data);
            }, limit)));
    }

    private byte[] post(URI endpoint, boolean needsSession, Payload payload, int limit)
        throws IOException
    {
        String session = token;
        if (needsSession && session == null)
        {
            throw new SessionExpired();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(bytes))
        {
            payload.write(data);
        }

        HttpsURLConnection connection = (HttpsURLConnection)endpoint.toURL().openConnection();
        try
        {
            connection.setSSLSocketFactory(tls);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            if (needsSession)
            {
                connection.setRequestProperty("Authorization", "Bearer " + session);
            }
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.size());
            try (var output = connection.getOutputStream())
            {
                bytes.writeTo(output);
            }
            return readResponse(connection, needsSession, limit);
        }
        finally
        {
            connection.disconnect();
        }
    }

    private static byte[] readResponse(HttpsURLConnection connection, boolean needsSession,
        int limit) throws IOException
    {
        int code = connection.getResponseCode();
        if (code == 401 && needsSession)
        {
            throw new SessionExpired();
        }
        if (code == 200)
        {
            try (InputStream response = connection.getInputStream())
            {
                return readBounded(response, limit);
            }
        }
        String message = null;
        try (InputStream error = connection.getErrorStream())
        {
            if (error != null)
            {
                message = new String(readBounded(error, 4096), StandardCharsets.UTF_8).trim();
            }
        }
        throw new IOException(message == null || message.isEmpty()
            ? "Spotter server returned HTTP " + code + "." : message);
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int size; (size = input.read(buffer)) != -1; )
        {
            if (size > limit - output.size())
            {
                throw new IOException("Spotter response exceeds the allowed size.");
            }
            output.write(buffer, 0, size);
        }
        return output.toByteArray();
    }
}

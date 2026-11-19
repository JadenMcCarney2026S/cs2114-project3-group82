package spotter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private final HttpClient http;
    private final URI address;
    private final String code;

    SpotterLanClient(URI address, String code)
    {
        this(address, code, null);
    }

    SpotterLanClient(URI address, String code, SSLContext tls)
    {
        if (!"https".equals(address.getScheme()) || address.getHost() == null
            || address.getUserInfo() != null || !"/chat".equals(address.getPath())
            || address.getQuery() != null || address.getFragment() != null
            || !code.matches("[A-Za-z0-9-]{1,64}"))
        {
            throw new IllegalArgumentException("Enter an HTTPS server URL ending in /chat and a demo code.");
        }
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3));
        if (tls != null)
        {
            builder.sslContext(tls);
        }
        http = builder.build();
        this.address = address;
        this.code = code;
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
        HttpRequest request = HttpRequest.newBuilder(address).timeout(Duration.ofSeconds(4))
            .header("Authorization", "Bearer " + code)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401)
        {
            throw new IOException("Invalid demo code.");
        }
        if (response.statusCode() != 200)
        {
            throw new IOException("Server rejected the request (HTTP " + response.statusCode() + ").");
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

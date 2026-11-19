package spotter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** Handles the account and profile endpoints on the LAN HTTPS server. */
final class SpotterAccountHttp implements HttpHandler
{
    private final SpotterAccounts accounts;
    private final Consumer<UserProfile> register;
    private final BiPredicate<Integer, Integer> canView;

    SpotterAccountHttp(SpotterAccounts accounts, Consumer<UserProfile> register,
        BiPredicate<Integer, Integer> canView)
    {
        this.accounts = accounts;
        this.register = register;
        this.canView = canView;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            if (!"/account".equals(exchange.getRequestURI().getPath()))
            {
                error(exchange, 404, "Unknown endpoint.");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod()))
            {
                error(exchange, 405, "POST required.");
                return;
            }
            try (DataInputStream input = new DataInputStream(exchange.getRequestBody()))
            {
                dispatch(exchange, input);
            }
        }
    }

    static String bearer(HttpExchange exchange)
    {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }

    private void dispatch(HttpExchange exchange, DataInputStream input) throws IOException
    {
        Reply reply;
        try
        {
            reply = switch (input.readUTF())
            {
                case "login" -> credentials(input, false);
                case "signup" -> credentials(input, true);
                case "complete" -> complete(exchange, input);
                case "profile" -> profile(exchange, input);
                case "logout" -> logout(exchange, input);
                default -> throw new IllegalArgumentException("Unknown account action.");
            };
        }
        catch (EOFException | UTFDataFormatException malformed)
        {
            reply = Reply.error(400, "Invalid account request.");
        }
        catch (IllegalArgumentException invalid)
        {
            reply = Reply.error(400, invalid.getMessage());
        }
        catch (SecurityException rejected)
        {
            reply = Reply.error(401, "Invalid credentials or expired session.");
        }
        catch (IllegalStateException limited)
        {
            reply = Reply.error(429, "Too many account attempts. Try again later.");
        }
        catch (IOException failure)
        {
            reply = Reply.error(500, "Could not process the account request.");
        }
        send(exchange, reply);
    }

    private Reply credentials(DataInputStream input, boolean signup) throws IOException
    {
        String username = input.readUTF();
        String password = input.readUTF();
        finish(input);
        SpotterAccounts.Session session = signup ? accounts.signUp(username, password)
            : accounts.login(username, password);
        return account(session, accounts.profile(session.userId()) != null);
    }

    private Reply complete(HttpExchange exchange, DataInputStream input) throws IOException
    {
        SpotterAccounts.Session session = requireSession(exchange);
        String name = input.readUTF();
        int age = input.readInt();
        String interest = input.readUTF();
        String bio = input.readUTF();
        int length = input.readInt();
        if (length <= 0 || length > SpotterAccounts.MAX_PHOTO_BYTES)
        {
            throw new IllegalArgumentException("Choose a profile photo of at most 2 MiB.");
        }
        byte[] photo = new byte[length];
        input.readFully(photo);
        finish(input);
        SpotterAccounts.Profile completed = accounts.complete(session.userId(), name, age,
            interest, bio, photo);
        register.accept(completed.user());
        return account(session, true);
    }

    private Reply profile(HttpExchange exchange, DataInputStream input) throws IOException
    {
        SpotterAccounts.Session session = requireSession(exchange);
        int userId = input.readInt();
        finish(input);
        if (!canView.test(session.userId(), userId))
        {
            return Reply.error(403, "This profile is unavailable.");
        }
        SpotterAccounts.Profile profile = accounts.profile(userId);
        if (profile == null)
        {
            return Reply.error(403, "This profile is unavailable.");
        }
        UserProfile user = profile.user();
        byte[] photo = profile.photo();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes))
        {
            output.writeUTF(user.getName());
            output.writeInt(user.getAge());
            output.writeUTF(user.getWorkoutExperience().keySet().iterator().next());
            output.writeUTF(user.getBio());
            output.writeInt(photo.length);
            output.write(photo);
        }
        return new Reply(200, bytes.toByteArray());
    }

    private Reply logout(HttpExchange exchange, DataInputStream input) throws IOException
    {
        finish(input);
        accounts.logout(bearer(exchange));
        return new Reply(200, new byte[0]);
    }

    private SpotterAccounts.Session requireSession(HttpExchange exchange)
    {
        SpotterAccounts.Session session = accounts.authenticate(bearer(exchange));
        if (session == null)
        {
            throw new SecurityException();
        }
        return session;
    }

    private static Reply account(SpotterAccounts.Session session, boolean complete) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes))
        {
            output.writeUTF(session.token());
            output.writeBoolean(complete);
        }
        return new Reply(200, bytes.toByteArray());
    }

    private static void finish(DataInputStream input) throws IOException
    {
        if (input.read() != -1)
        {
            throw new IllegalArgumentException("Unexpected account request data.");
        }
    }

    private record Reply(int status, byte[] body)
    {
        static Reply error(int status, String message)
        {
            return new Reply(status, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void error(HttpExchange exchange, int status, String message) throws IOException
    {
        send(exchange, Reply.error(status, message));
    }

    private static void send(HttpExchange exchange, Reply reply) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", reply.status() == 200
            ? "application/octet-stream" : "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(reply.status(), reply.body().length == 0 ? -1 : reply.body().length);
        if (reply.body().length != 0)
        {
            exchange.getResponseBody().write(reply.body());
        }
    }
}

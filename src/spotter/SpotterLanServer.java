package spotter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;

/** Hosts Spotter accounts and shared LAN state on an explicitly selected network interface. */
public final class SpotterLanServer implements AutoCloseable
{
    static final int HISTORY_LIMIT = 100;
    private static final int SEND_ID_LIMIT = 256;

    private record Request(String action, int peer, String message, String sendId, int after)
    {
    }

    private record SentMessage(int peer, String text)
    {
    }

    private final ChatManager chat = new ChatManager(new MatchManager(), HISTORY_LIMIT);
    private final Map<Integer, MainLogic> sessions = new LinkedHashMap<>();
    private final Map<Integer, Map<String, SentMessage>> sendIds = new LinkedHashMap<>();
    private final SpotterAccounts accounts;
    private final HttpsServer server;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(8, 8, 0,
        TimeUnit.SECONDS, new ArrayBlockingQueue<>(32));

    SpotterLanServer(String host, int port, SSLContext tls, Path accountFile) throws IOException
    {
        accounts = new SpotterAccounts(accountFile);
        accounts.profiles().forEach(profile -> register(profile.user()));
        configureHttpLimits();
        server = HttpsServer.create(new InetSocketAddress(host, port), 16);
        server.setHttpsConfigurator(new HttpsConfigurator(tls));
        server.setExecutor(workers);
        server.createContext("/chat", this::handle);
        server.createContext("/account", new SpotterAccountHttp(accounts, this::register,
            this::canView));
        server.start();
    }

    int port()
    {
        return server.getAddress().getPort();
    }

    private synchronized void register(UserProfile user)
    {
        if (sessions.containsKey(user.getUserId()))
        {
            return;
        }
        MainLogic newSession = new MainLogic(user, chat);
        for (MainLogic existing : sessions.values())
        {
            newSession.addProfile(existing.getCurrentUser());
            existing.addProfile(user);
        }
        sessions.put(user.getUserId(), newSession);
    }

    private synchronized boolean canView(Integer actor, Integer target)
    {
        MainLogic session = sessions.get(actor);
        return session != null && session.canViewProfile(target);
    }

    private synchronized MainLogic session(int userId)
    {
        return sessions.get(userId);
    }

    private void handle(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            if (!"/chat".equals(exchange.getRequestURI().getPath()))
            {
                respond(exchange, 404, "Unknown endpoint");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod()))
            {
                respond(exchange, 405, "POST required");
                return;
            }
            SpotterAccounts.Session account = accounts.authenticate(SpotterAccountHttp.bearer(exchange));
            if (account == null)
            {
                respond(exchange, 401, "Log in to continue.");
                return;
            }
            MainLogic session = session(account.userId());
            if (session == null)
            {
                respond(exchange, 403, "Complete your profile first.");
                return;
            }
            byte[] body = exchange.getRequestBody().readNBytes(4097);
            if (body.length > 4096)
            {
                respond(exchange, 413, "Request too large");
                return;
            }
            Request request;
            try
            {
                request = parse(body);
            }
            catch (IOException | IllegalArgumentException malformed)
            {
                respond(exchange, 400, "Invalid request");
                return;
            }
            respond(exchange, 200, snapshot(session, request));
        }
    }

    private static Request parse(byte[] body) throws IOException
    {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body)))
        {
            String action = input.readUTF();
            int peer = input.readInt();
            String message = input.readUTF();
            String sendId = input.readUTF();
            int after = input.readInt();
            boolean sending = "send".equals(action);
            if (input.available() != 0 || message.length() > 500 || after < 0 || peer < 0
                || message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0
                || (sending ? !UUID.fromString(sendId).toString().equals(sendId)
                    : !sendId.isEmpty() || !message.isEmpty())
                || !java.util.Set.of("send", "view", "left", "right").contains(action))
            {
                throw new IllegalArgumentException("Invalid request");
            }
            return new Request(action, peer, message, sendId, after);
        }
    }

    private synchronized String snapshot(MainLogic session, Request request)
    {
        UserProfile candidate = session.getNextProfile();
        boolean accepted = switch (request.action())
        {
            case "view" -> true;
            case "send" -> sendOnce(session, request);
            case "left", "right" -> {
                if (candidate == null || candidate.getUserId() != request.peer())
                {
                    yield false;
                }
                session.swipeCurrentProfile("right".equals(request.action())
                    ? SwipeAction.RIGHT : SwipeAction.LEFT);
                yield true;
            }
            default -> throw new IllegalStateException("Unvalidated action");
        };
        candidate = session.getNextProfile();
        String matches = session.getMatches().stream()
            .map(profile -> Integer.toString(profile.getUserId()))
            .collect(Collectors.joining(","));
        ChatManager.History history = chat.getHistory(session.getCurrentUser().getUserId(),
            request.peer(), request.after());
        return accepted + "\n" + (candidate == null ? 0 : candidate.getUserId())
            + "\n" + matches + "\n" + history.start() + "\n" + history.next()
            + "\n" + String.join("\n", history.messages());
    }

    private boolean sendOnce(MainLogic session, Request request)
    {
        Map<String, SentMessage> recent = sendIds.computeIfAbsent(
            session.getCurrentUser().getUserId(), userId -> new LinkedHashMap<>());
        SentMessage message = new SentMessage(request.peer(), request.message());
        if (recent.containsKey(request.sendId()))
        {
            return message.equals(recent.get(request.sendId()));
        }
        if (request.message().isBlank() || !session.sendMessage(request.peer(), request.message()))
        {
            return false;
        }
        recent.put(request.sendId(), message);
        if (recent.size() > SEND_ID_LIMIT)
        {
            recent.remove(recent.keySet().iterator().next());
        }
        return true;
    }

    private static void configureHttpLimits()
    {
        // shortcut: limits use the Java 17 HTTP server's JVM-wide settings.
        Map.of("sun.net.httpserver.maxReqTime", "5", "sun.net.httpserver.maxRspTime", "5",
            "jdk.httpserver.maxConnections", "32").forEach((name, value) ->
            System.setProperty(name, System.getProperty(name, value)));
    }

    private static void respond(HttpExchange exchange, int status, String content)
        throws IOException
    {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close()
    {
        server.stop(0);
        workers.shutdownNow();
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException
    {
        if (args.length < 3 || args.length > 4)
        {
            System.out.println("Usage: SpotterLanServer <host LAN IP> <port> <PKCS12 path> [account store path]");
            return;
        }
        char[] password = readPassword();
        SSLContext tls;
        try
        {
            tls = SpotterLanTls.serverContext(Path.of(args[2]), password);
        }
        finally
        {
            Arrays.fill(password, '\0');
        }
        Path accountFile = args.length == 4 ? Path.of(args[3])
            : Path.of(System.getProperty("user.home"), ".spotter", "accounts.dat");
        SpotterLanServer host = new SpotterLanServer(args[0], Integer.parseInt(args[1]), tls,
            accountFile);
        System.out.printf("Spotter LAN server listening at https://%s:%d/chat%n",
            args[0], host.port());
    }

    private static char[] readPassword()
    {
        String configured = System.getenv("SPOTTER_KEYSTORE_PASSWORD");
        char[] password = configured != null ? configured.toCharArray()
            : System.console() == null ? null : System.console().readPassword("Keystore password: ");
        if (password == null || password.length == 0)
        {
            throw new IllegalArgumentException("Enter the keystore password or set SPOTTER_KEYSTORE_PASSWORD.");
        }
        return password;
    }

}

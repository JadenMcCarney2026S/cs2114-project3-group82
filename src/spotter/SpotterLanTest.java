package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class SpotterLanTest
{
    @TempDir
    Path tempDir;

    @Test
    void independentClientsShareOnlyAuthorizedConversations() throws Exception
    {
        Path keystore = tempDir.resolve("server.p12");
        char[] password = UUID.randomUUID().toString().toCharArray();
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process generated = new ProcessBuilder(keytool, "-genkeypair", "-alias", "spotter",
            "-keyalg", "RSA", "-dname", "CN=spotter.test", "-ext", "SAN=ip:127.0.0.1",
            "-storetype", "PKCS12", "-keystore", keystore.toString(),
            "-storepass", new String(password), "-validity", "1")
            .redirectErrorStream(true).start();
        assertEquals(0, generated.waitFor(), new String(generated.getInputStream().readAllBytes()));
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(keystore))
        {
            trustStore.load(input, password);
        }
        TrustManagerFactory trust = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustStore);
        SSLContext clientTls = SSLContext.getInstance("TLS");
        clientTls.init(null, trust.getTrustManagers(), null);
        try (SpotterLanServer server = new SpotterLanServer("127.0.0.1", 0,
            SpotterLanTls.serverContext(keystore, password)))
        {
            URI address = URI.create("https://127.0.0.1:" + server.port() + "/chat");
            assertThrows(SSLHandshakeException.class,
                () -> new SpotterLanClient(address, server.code(1)).request("view", 0, ""));
            URI wrongHost = URI.create("https://localhost:" + server.port() + "/chat");
            assertThrows(SSLHandshakeException.class,
                () -> new SpotterLanClient(wrongHost, server.code(1), clientTls)
                    .request("view", 0, ""));
            SpotterLanClient alice = new SpotterLanClient(address, server.code(1), clientTls);
            SpotterLanClient bob = new SpotterLanClient(address, server.code(2), clientTls);
            SpotterLanClient carol = new SpotterLanClient(address, server.code(3), clientTls);
            assertFalse(alice.request("send", 2, "Before match").accepted());
            assertTrue(alice.request("right", 2, "").accepted());
            assertFalse(alice.request("right", 2, "").accepted());
            assertTrue(bob.request("right", 1, "").accepted());
            assertEquals(java.util.List.of(2), alice.request("view", 2, "").matches());
            assertFalse(alice.request("send", 2, "   ").accepted());
            assertThrows(java.io.IOException.class,
                () -> alice.request("send", 2, "a".repeat(501)));
            assertThrows(IllegalArgumentException.class,
                () -> new SpotterLanClient(URI.create("http://127.0.0.1/chat"), server.code(1)));
            String sendId = UUID.randomUUID().toString();
            assertTrue(alice.request("send", 2, "Hi Bob", sendId, 0).accepted());
            assertTrue(alice.request("send", 2, "Hi Bob", sendId, 0).accepted());
            assertFalse(alice.request("send", 2, "Changed", sendId, 0).accepted());
            assertEquals("1: Hi Bob", bob.request("view", 1, "").history());
            assertTrue(bob.request("send", 1, "Hi Alice").accepted());
            assertEquals("1: Hi Bob\n2: Hi Alice", alice.request("view", 2, "").history());
            assertEquals("2: Hi Alice", alice.request("view", 2, "", "", 1).history());
            for (int i = 0; i < 100; i++)
            {
                assertTrue(alice.request("send", 2, "Message " + i).accepted());
            }
            var retained = bob.request("view", 1, "", "", 0);
            assertEquals(2, retained.start());
            assertEquals(102, retained.next());
            assertEquals(100, retained.history().lines().count());
            assertEquals("", bob.request("view", 1, "", "", retained.next()).history());
            assertEquals("", carol.request("view", 1, "").history());
            assertFalse(carol.request("send", 1, "Private").accepted());
            assertThrows(java.io.IOException.class,
                () -> new SpotterLanClient(address, "invalid", clientTls).request("view", 0, ""));
        }
    }
}

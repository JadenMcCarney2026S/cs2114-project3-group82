package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.UUID;
import javax.imageio.ImageIO;
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
        SSLContext serverTls = SpotterLanTls.serverContext(keystore, password);
        Path accounts = tempDir.resolve("accounts.dat");
        String alicePassword = UUID.randomUUID().toString();
        String bobPassword = UUID.randomUUID().toString();
        byte[] photo = photo();
        try (SpotterLanServer server = new SpotterLanServer("127.0.0.1", 0, serverTls, accounts))
        {
            URI address = URI.create("https://127.0.0.1:" + server.port() + "/chat");
            assertThrows(SSLHandshakeException.class,
                () -> new SpotterLanClient(address).signUp("alice", UUID.randomUUID().toString()));
            URI wrongHost = URI.create("https://localhost:" + server.port() + "/chat");
            assertThrows(SSLHandshakeException.class,
                () -> new SpotterLanClient(wrongHost, clientTls)
                    .signUp("alice", UUID.randomUUID().toString()));
            SpotterLanClient alice = new SpotterLanClient(address, clientTls);
            SpotterLanClient bob = new SpotterLanClient(address, clientTls);
            SpotterLanClient carol = new SpotterLanClient(address, clientTls);
            assertFalse(alice.signUp("alice", alicePassword).profileComplete());
            assertFalse(bob.signUp("bob", bobPassword).profileComplete());
            assertThrows(IOException.class, () -> alice.request("view", 0, ""));
            assertThrows(IOException.class, () -> alice.profile(2));
            assertThrows(IOException.class, () -> alice.completeProfile("Alice", 21, "Running",
                "", new byte[] { 1, 2, 3 }));
            assertTrue(alice.completeProfile("Alice", 21, "Running", "Hello", photo).profileComplete());
            assertEquals(0, alice.request("view", 0, "").candidate());
            assertThrows(IOException.class, () -> alice.profile(2));
            bob.logout();
            assertFalse(bob.login("bob", bobPassword).profileComplete());
            assertTrue(bob.completeProfile("Bob", 22, "running", "", photo).profileComplete());
            assertEquals(2, alice.request("view", 0, "").candidate());
            assertEquals("Bob", alice.profile(2).name());
            assertTrue(alice.profile(2).photo().length > 0);
            assertEquals("Hello", bob.profile(1).bio());
            carol.signUp("carol", UUID.randomUUID().toString());
            carol.completeProfile("Carol", 21, "Swimming", "", photo);
            assertThrows(IOException.class, () -> carol.profile(1));
            assertEquals(0, carol.request("view", 0, "").candidate());
            assertFalse(alice.request("send", 2, "Before match").accepted());
            assertTrue(alice.request("right", 2, "").accepted());
            assertFalse(alice.request("right", 2, "").accepted());
            assertTrue(bob.request("right", 1, "").accepted());
            assertEquals(java.util.List.of(2), alice.request("view", 2, "").matches());
            assertFalse(alice.request("send", 2, "   ").accepted());
            assertThrows(java.io.IOException.class,
                () -> alice.request("send", 2, "a".repeat(501)));
            assertThrows(IllegalArgumentException.class,
                () -> new SpotterLanClient(URI.create("http://127.0.0.1/chat")));
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
            assertThrows(SpotterLanClient.SessionExpiredException.class,
                () -> new SpotterLanClient(address, clientTls).request("view", 0, ""));
        }
        try (SpotterLanServer restarted = new SpotterLanServer("127.0.0.1", 0, serverTls, accounts))
        {
            URI address = URI.create("https://127.0.0.1:" + restarted.port() + "/chat");
            SpotterLanClient alice = new SpotterLanClient(address, clientTls);
            assertTrue(alice.login("alice", alicePassword).profileComplete());
            assertEquals(2, alice.request("view", 0, "").candidate());
            assertEquals("Bob", alice.profile(2).name());
            assertEquals("", alice.request("view", 2, "").history());
        }
    }

    private static byte[] photo() throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}

package spotter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpotterAccountsTest
{
    @TempDir
    Path temp;

    @Test
    void persistsProfileAndCredentialsButNotSessions() throws Exception
    {
        Path store = temp.resolve("accounts.bin");
        SpotterAccounts first = new SpotterAccounts(store);
        String password = UUID.randomUUID().toString();
        var signedUp = first.signUp(" Alice_01 ", password);
        assertTrue(first.profiles().isEmpty());
        assertFalse(new String(Files.readAllBytes(store), StandardCharsets.ISO_8859_1)
            .contains(password));
        assertThrows(IllegalArgumentException.class, () -> first.signUp("alice_01", password));
        assertThrows(IllegalArgumentException.class, () -> first.complete(signedUp.userId(),
            "Alice", 22, "Running", "", new byte[] { 0, 1, 2 }));
        assertThrows(IllegalArgumentException.class, () -> first.complete(signedUp.userId(),
            "Alice", 22, "Running", "", photo(4097)));
        byte[] validPhoto = photo(800);
        assertThrows(IllegalArgumentException.class, () -> first.complete(signedUp.userId(),
            "Alice", 17, "Running", "", validPhoto));
        assertNull(first.profile(signedUp.userId()));
        var completed = first.complete(signedUp.userId(), "Alice", 18, "Running", "Hi", validPhoto);
        assertThrows(IllegalArgumentException.class, () -> first.complete(signedUp.userId(),
            "Other", 25, "Running", "", validPhoto));
        assertEquals(Map.of("running", "Unspecified"), completed.user().getWorkoutExperience());
        assertEquals(512, ImageIO.read(new ByteArrayInputStream(completed.photo())).getWidth());
        completed.user().setName("Tampered");
        byte[] exposed = completed.photo();
        exposed[0] = 0;
        assertNotEquals(0, first.profile(signedUp.userId()).photo()[0]);

        SpotterAccounts reopened = new SpotterAccounts(store);
        assertNull(reopened.authenticate(signedUp.token()));
        var signedIn = reopened.login("ALICE_01", password);
        assertEquals(signedUp.userId(), signedIn.userId());
        assertEquals("Alice", reopened.profiles().get(0).user().getName());
        assertEquals("Hi", reopened.profiles().get(0).user().getBio());
        reopened.logout(signedIn.token());
        assertNull(reopened.authenticate(signedIn.token()));
        assertThrows(SecurityException.class, () -> reopened.login("alice_01", "incorrect-password"));
    }

    @Test
    void rejectsInvalidCredentialsAndOversizedUploadsWithoutSaving()
        throws Exception
    {
        SpotterAccounts accounts = new SpotterAccounts(temp.resolve("accounts.bin"));
        String password = UUID.randomUUID().toString();
        assertThrows(IllegalArgumentException.class, () -> accounts.signUp("a?", password));
        assertThrows(IllegalArgumentException.class, () -> accounts.signUp("runner", "short"));
        assertThrows(IllegalArgumentException.class,
            () -> accounts.signUp("runner", "x".repeat(129)));
        var session = accounts.signUp("runner", password);
        assertThrows(IllegalArgumentException.class, () -> accounts.complete(session.userId(),
            "Runner", 21, "Running", "", new byte[SpotterAccounts.MAX_PHOTO_BYTES + 1]));
        assertNull(accounts.profile(session.userId()));
    }

    @Test
    void expiresSessionsAndLimitsRepeatedFailures() throws Exception
    {
        TestClock clock = new TestClock();
        SpotterAccounts accounts = new SpotterAccounts(temp.resolve("accounts.bin"), clock);
        var session = accounts.signUp("runner", UUID.randomUUID().toString());
        assertEquals(session, accounts.authenticate(session.token()));
        for (int i = 0; i < 8; i++)
        {
            assertThrows(SecurityException.class, () -> accounts.login("runner", "incorrect-password"));
        }
        assertThrows(IllegalStateException.class, () -> accounts.login("runner", "incorrect-password"));
        clock.now = clock.now.plusSeconds(43_201);
        assertNull(accounts.authenticate(session.token()));
        assertThrows(SecurityException.class, () -> accounts.login("runner", "incorrect-password"));
    }

    private static byte[] photo(int width) throws Exception
    {
        BufferedImage image = new BufferedImage(width, 200, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static final class TestClock extends Clock
    {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant()
        {
            return now;
        }
    }
}

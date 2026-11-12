package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpotterControllerTest
{
    private ByteArrayOutputStream output;
    private MainLogic alice;
    private MainLogic bob;
    private SpotterController controller;

    @BeforeEach
    void setUp()
    {
        UserProfile a = new UserProfile(1, "Alice", 20, Map.of("Running", "Beginner"));
        UserProfile b = new UserProfile(2, "Bob", 21, Map.of("Running", "Beginner"));
        ChatManager chat = new ChatManager(new MatchManager());
        alice = new MainLogic(a, chat);
        bob = new MainLogic(b, chat);
        alice.addProfile(b);
        bob.addProfile(a);
        output = new ByteArrayOutputStream();
        controller = new SpotterController(Map.of(1, alice, 2, bob), 1,
            new SpotterView(new PrintStream(output)));
    }

    @Test
    void testConsoleWorkflowUsesRealReciprocalSwipes()
    {
        controller.run(new Scanner("profile\nright\nmatches\nuser 2\nright\n"
            + "matches\nsend 1 Hello Alice\nuser 1\nsend 2 Hi Bob\nchat 2\nquit\n"));
        String text = output.toString();
        assertTrue(text.contains("Profile 2: Bob"));
        assertTrue(text.contains("No matches yet."));
        assertTrue(text.contains("New match: Alice"));
        assertTrue(text.contains("Match 1: Alice"));
        assertTrue(text.contains("2: Hello Alice"));
        assertTrue(text.contains("1: Hi Bob"));
        assertEquals(alice.getConversation(2), bob.getConversation(1));
        assertEquals(2, alice.getConversation(2).size());
        assertNull(alice.getNextProfile());
        assertNull(bob.getNextProfile());
    }

    @Test
    void testInvalidInputDoesNotConsumeProfileOrRevealMessages()
    {
        controller.run(new Scanner("\nunknown\nright extra\nuser 999\nuser x\n"
            + "user 999999999999999999999\nchat\nchat x\nchat 2 extra\nsend\n"
            + "send x hello\nsend 2\nsend 2 hello\nchat 2\n"));
        assertFalse(controller.handleSwipe(null));
        assertTrue(controller.handleCommand(null));
        assertEquals(2, alice.getNextProfile().getUserId());
        assertTrue(alice.getMatches().isEmpty());
        assertTrue(alice.getConversation(2).isEmpty());
        assertTrue(output.toString().contains("Error:"));
        assertTrue(output.toString().contains("No accessible messages."));
        assertTrue(output.toString().contains("Goodbye."));
    }

    @Test
    void testEmptyQueueAndBlankMessageAfterMatching()
    {
        controller.handleCommand("right");
        controller.handleCommand("user 2");
        controller.handleCommand("right");
        controller.run(new Scanner("right\nleft\nprofile\nsend 1   \nchat 1\nquit\n"));
        assertTrue(alice.getConversation(2).isEmpty());
        assertTrue(output.toString().contains("No more profiles."));
        assertTrue(output.toString().contains("Message rejected"));
    }
}

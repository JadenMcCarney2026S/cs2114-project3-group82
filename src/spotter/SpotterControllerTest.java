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
        WorkoutManager workouts = new WorkoutManager(chat.getMatchManager());
        alice = new MainLogic(a, chat, workouts);
        bob = new MainLogic(b, chat, workouts);
        alice.addProfile(b);
        bob.addProfile(a);
        output = new ByteArrayOutputStream();
        controller = new SpotterController(Map.of(1, alice, 2, bob), 1,
            new SpotterView(new PrintStream(output)));
    }

    @Test
    void testWorkoutCommandsEnforceMatchingAndValidateInput()
    {
        controller.run(new Scanner("exercise 2 3 8 0 Hidden exercise\nworkout 2\n"
            + "right\nuser 2\nright\nexercise 1 3 8 0 Push ups\n"
            + "exercise 1 two 8 0 Bad sets\nexercise 1 3 0 0 Bad reps\n"
            + "exercise 1 3 8 Infinity Bad weight\nexercise 1 3 8 0\n"
            + "user 1\nworkout 2\nquit\n"));

        String text = output.toString();
        assertTrue(text.contains("Use a mutual match"));
        assertTrue(text.contains("Sets and reps must be whole numbers"));
        assertTrue(text.contains("Usage: exercise"));
        assertTrue(text.contains("Push ups"));
        assertFalse(text.contains("Hidden exercise"));
        assertEquals(1, alice.getWorkout(2).size());
        assertEquals(alice.getWorkout(2), bob.getWorkout(1));
    }

    @Test
    void testWorkoutRejectsMalformedTargetsAndMissingNumericFields()
    {
        controller.run(new Scanner("right\nuser 2\nright\n"
            + "exercise 1 3 8 0 Push ups\n"
            + "exercise x 3 8 0 Bad target\nworkout 2147483648\nworkout 1 extra\n"
            + "exercise 1 2147483648 8 0 Overflow\nworkout 1\n"));
        assertFalse(controller.addWorkoutExercise(1, "Row", null, "8", "20"));
        assertFalse(controller.addWorkoutExercise(1, "Row", "3", null, "20"));
        assertFalse(controller.addWorkoutExercise(1, "Row", "3", "8", null));

        String text = output.toString();
        assertTrue(text.contains("User ID must be a 32-bit integer"));
        assertTrue(text.contains("Usage: workout"));
        assertTrue(text.contains("Sets and reps must be whole numbers"));
        assertTrue(text.contains("Enter sets, reps, and weight"));
        assertEquals(java.util.List.of(new Workout.Exercise("Push ups", 3, 8, 0)),
            alice.getWorkout(2));
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

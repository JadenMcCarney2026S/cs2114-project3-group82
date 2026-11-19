package spotter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

class MainLogicTest
{
    private UserProfile alice;
    private UserProfile bob;
    private UserProfile carol;
    private MatchManager matches;
    private ChatManager chat;
    private WorkoutManager workouts;
    private MainLogic aliceLogic;
    private MainLogic bobLogic;

    @BeforeEach
    void setUp()
    {
        alice = profile(1, "Alice", "Running");
        bob = profile(2, "Bob", "Running");
        carol = profile(3, "Carol", "Running");
        matches = new MatchManager();
        chat = new ChatManager(matches);
        workouts = new WorkoutManager(matches);
        aliceLogic = new MainLogic(alice, chat, workouts);
        bobLogic = new MainLogic(bob, chat, workouts);
        assertTrue(aliceLogic.addProfile(bob));
        assertTrue(bobLogic.addProfile(alice));
    }

    private UserProfile profile(int id, String name, String activity)
    {
        return new UserProfile(id, name, 20, Map.of(activity, "Beginner"));
    }

    private void matchAliceAndBob()
    {
        assertNull(aliceLogic.swipeCurrentProfile(SwipeAction.RIGHT));
        assertSame(alice, bobLogic.swipeCurrentProfile(SwipeAction.RIGHT));
    }

    @Test
    void workoutIsSharedInOrderOnlyByMatchedUsers()
    {
        assertFalse(aliceLogic.addWorkoutExercise(2, "Squat", 3, 10, 0));
        assertTrue(aliceLogic.getWorkout(2).isEmpty());
        matchAliceAndBob();
        assertTrue(aliceLogic.addWorkoutExercise(2, " Squat ", 3, 10, 0));
        assertTrue(bobLogic.addWorkoutExercise(1, "Deadlift", 2, 5, 45.5));
        var expected = List.of(new Workout.Exercise("Squat", 3, 10, 0),
            new Workout.Exercise("Deadlift", 2, 5, 45.5));
        assertEquals(expected, aliceLogic.getWorkout(2));
        assertEquals(expected, bobLogic.getWorkout(1));
        assertThrows(UnsupportedOperationException.class, () -> bobLogic.getWorkout(1).clear());
        MainLogic outsider = new MainLogic(carol, chat, workouts);
        assertTrue(outsider.getWorkout(1).isEmpty());
        assertFalse(outsider.addWorkoutExercise(1, "Row", 1, 10, 20));
    }

    @Test
    void workoutUsesTheSameAuthorizationAsChat()
    {
        assertThrows(IllegalArgumentException.class, () -> new MainLogic(alice, chat, null));
        assertThrows(IllegalArgumentException.class,
            () -> new MainLogic(alice, chat, new WorkoutManager(new MatchManager())));
        assertSame(workouts, aliceLogic.getWorkoutManager());
    }

    @Test
    void testOneSidedLikeAdvancesQueueWithoutMatch()
    {
        assertTrue(aliceLogic.addProfile(carol));
        assertSame(bob, aliceLogic.getNextProfile());
        assertSame(bob, aliceLogic.getNextProfile());
        assertNull(aliceLogic.swipeCurrentProfile(SwipeAction.RIGHT));
        assertSame(carol, aliceLogic.getNextProfile());
        assertTrue(matches.hasSwiped(1, 2));
        assertTrue(aliceLogic.getMatches().isEmpty());
        assertTrue(bobLogic.getMatches().isEmpty());
        assertFalse(aliceLogic.sendMessage(2, "Too early"));
    }

    @Test
    void testMutualLikesAndAuthorizedMessaging()
    {
        matchAliceAndBob();
        assertEquals(List.of(bob), aliceLogic.getMatches());
        assertEquals(List.of(alice), bobLogic.getMatches());
        assertTrue(aliceLogic.sendMessage(2, "Meet at the gym?"));
        assertTrue(bobLogic.sendMessage(1, "Yes!"));
        List<String> expected = List.of("1: Meet at the gym?", "2: Yes!");
        assertEquals(expected, aliceLogic.getConversation(2));
        assertEquals(expected, bobLogic.getConversation(1));
        aliceLogic.getConversation(2).clear();
        aliceLogic.getMatches().clear();
        assertEquals(expected, bobLogic.getConversation(1));
        assertEquals(List.of(bob), aliceLogic.getMatches());
    }

    @Test
    void testLeftSwipeDoesNotMatchReciprocalLike()
    {
        assertNull(bobLogic.swipeCurrentProfile(SwipeAction.RIGHT));
        assertNull(aliceLogic.swipeCurrentProfile(SwipeAction.LEFT));
        assertTrue(matches.hasSwiped(1, 2));
        assertFalse(matches.areMatched(1, 2));
        assertNull(aliceLogic.getNextProfile());
        assertTrue(aliceLogic.getMatches().isEmpty());
    }

    @Test
    void testNullSwipeLeavesQueueAndHistoryUnchanged()
    {
        assertTrue(aliceLogic.addProfile(carol));
        assertNull(aliceLogic.swipeCurrentProfile(null));
        assertSame(bob, aliceLogic.getNextProfile());
        assertFalse(matches.hasSwiped(1, 2));
        assertFalse(matches.hasSwiped(1, 3));
        assertNull(bobLogic.swipeCurrentProfile(SwipeAction.RIGHT));
        assertSame(bob, aliceLogic.swipeCurrentProfile(SwipeAction.RIGHT));
        assertSame(carol, aliceLogic.getNextProfile());
    }

    @Test
    void testSwipedProfileCannotBeReaddedEvenInNewUserSession()
    {
        aliceLogic.swipeCurrentProfile(SwipeAction.LEFT);
        assertFalse(aliceLogic.addProfile(bob));
        assertFalse(aliceLogic.addProfile(profile(2, "Bob copy", "Running")));
        MainLogic reopened = new MainLogic(alice, chat);
        assertFalse(reopened.addProfile(bob));
        assertNull(reopened.getNextProfile());
    }

    @Test
    void testFiltersSelfDuplicatesAndUnsharedInterests()
    {
        assertFalse(aliceLogic.addProfile(null));
        assertFalse(aliceLogic.addProfile(alice));
        assertFalse(aliceLogic.addProfile(profile(2, "Bob copy", "Running")));
        assertFalse(aliceLogic.addProfile(profile(4, "Dan", "Swimming")));
        aliceLogic.swipeCurrentProfile(SwipeAction.LEFT);
        assertNull(aliceLogic.getNextProfile());
        assertFalse(aliceLogic.addProfile(profile(5, "Eve", "Swimming")));
        assertNull(aliceLogic.getNextProfile());
    }

    @Test
    void testEmptyQueueDoesNotRecordSwipeOrCrash()
    {
        MainLogic empty = new MainLogic(carol, chat);
        assertNull(empty.getNextProfile());
        assertNull(empty.swipeCurrentProfile(SwipeAction.RIGHT));
        assertNull(empty.swipeCurrentProfile(SwipeAction.LEFT));
        assertNull(empty.swipeCurrentProfile(null));
        assertFalse(matches.hasSwiped(3, 1));
        assertFalse(matches.hasSwiped(3, 2));
        assertTrue(empty.getMatches().isEmpty());
    }

    @Test
    void testInvalidMessagesDoNotChangeConversation()
    {
        matchAliceAndBob();
        assertTrue(aliceLogic.sendMessage(2, "Hello"));
        assertFalse(aliceLogic.sendMessage(2, ""));
        assertFalse(aliceLogic.sendMessage(2, "  \t"));
        assertFalse(aliceLogic.sendMessage(2, null));
        assertFalse(aliceLogic.sendMessage(3, "Unmatched"));
        assertEquals(List.of("1: Hello"), bobLogic.getConversation(1));
        assertTrue(aliceLogic.getConversation(3).isEmpty());
    }

    @Test
    void testUnauthorizedUserCannotSeeExistingPrivateMessages()
    {
        matchAliceAndBob();
        aliceLogic.sendMessage(2, "Private");
        MainLogic outsider = new MainLogic(carol, chat);
        assertTrue(outsider.getConversation(1).isEmpty());
        assertTrue(outsider.getConversation(2).isEmpty());
        assertFalse(outsider.sendMessage(2, "Intrusion"));
        assertEquals(List.of("1: Private"), bobLogic.getConversation(1));
    }

    @Test
    void testDuplicateSwipeCannotChangeLeftToRight()
    {
        assertFalse(matches.recordSwipe(1, 2, SwipeAction.LEFT));
        assertFalse(matches.recordSwipe(2, 1, SwipeAction.RIGHT));
        assertFalse(matches.recordSwipe(1, 2, SwipeAction.RIGHT));
        assertFalse(matches.areMatched(1, 2));
        assertFalse(matches.recordSwipe(3, 1, null));
        assertFalse(matches.hasSwiped(3, 1));
    }

    @Test
    void testStandaloneConstructorAndInvalidDependencies()
    {
        MainLogic standalone = new MainLogic(alice);
        assertTrue(standalone.addProfile(bob));
        assertSame(bob, standalone.getNextProfile());
        assertNull(standalone.swipeCurrentProfile(SwipeAction.RIGHT));
        assertNull(standalone.getNextProfile());
        assertThrows(IllegalArgumentException.class, () -> new MainLogic(null));
        assertThrows(IllegalArgumentException.class, () -> new MainLogic(alice, null));
        assertThrows(IllegalArgumentException.class, () -> new ChatManager(null));
    }
}

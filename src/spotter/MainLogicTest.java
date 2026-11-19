package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
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
        aliceLogic = new MainLogic(alice, chat);
        bobLogic = new MainLogic(bob, chat);
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
    
    @Test
    public void testCreateWorkout()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
        
        MainLogic model = new MainLogic(user);
        
        assertTrue(model.createWorkout("Push Day"));
        assertEquals("Push Day", model.getWorkout().getWorkoutName());
            
    }
    
    @Test
    public void testCreateWorkoutInvalid()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
        
        MainLogic model = new MainLogic(user);
        
        assertFalse(model.createWorkout(null));
        assertFalse(model.createWorkout(""));
        assertFalse(model.createWorkout("   "));
        
        assertNull(model.getWorkout());
    }
    

    @Test
    public void testAddExercise()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
    
        MainLogic model = new MainLogic(user);
    
        assertTrue(model.createWorkout("Push Day"));
        assertTrue(model.addExercise("Bench Press"));
        assertTrue(model.addExercise("Shoulder Press"));
    
        assertEquals(2, model.getWorkout().getExercises().size());
        assertEquals("Bench Press",
            model.getWorkout().getExercises().get(0));
        assertEquals("Shoulder Press",
            model.getWorkout().getExercises().get(1));
    }
    
    @Test
    public void testAddExerciseInvalid()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());

        MainLogic model = new MainLogic(user);

        assertTrue(model.createWorkout("Push Day"));

        assertFalse(model.addExercise(null));
        assertFalse(model.addExercise(""));
        assertFalse(model.addExercise("   "));

        assertTrue(model.getWorkout().getExercises().isEmpty());
    }
    
    
    @Test
    public void testAddExerciseWithoutWorkout()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
    
        MainLogic model = new MainLogic(user);
    
        assertFalse(model.addExercise("Bench Press"));
        assertNull(model.getWorkout());
    }
    
    @Test
    public void testGetWorkoutBeforeCreation()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
    
        MainLogic model = new MainLogic(user);
    
        assertNull(model.getWorkout());
    }
    
    @Test
    public void testReplaceExistingWorkout()
    {
        UserProfile user = new UserProfile(
            1, "Grace", 20, new HashMap<>());
    
        MainLogic model = new MainLogic(user);
    
        assertTrue(model.createWorkout("Push Day"));
        assertTrue(model.addExercise("Bench Press"));
    
        assertTrue(model.createWorkout("Leg Day"));
    
        assertEquals("Leg Day", model.getWorkout().getWorkoutName());
        assertTrue(model.getWorkout().getExercises().isEmpty());
    }

}

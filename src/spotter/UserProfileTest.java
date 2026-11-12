package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileTest
{
    @Test
    void testOptionalDetailsAndOriginalConstructor()
    {
        UserProfile original = new UserProfile(1, "Alice", 20, Map.of());
        assertEquals("", original.getBio());
        UserProfile detailed = new UserProfile(2, "Bob", 21, Map.of(),
            " Get stronger ", "Campus", "Evening", "Hello");
        assertEquals("Get stronger", detailed.getGoal());
        assertEquals("Campus", detailed.getGym());
        assertEquals("Evening", detailed.getPreferredTime());
        assertEquals("Hello", detailed.getBio());
    }

    @Test
    void testInvalidProfileInput()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, " ", 20, Map.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, "Alice", 0, Map.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, "Alice", 20, Map.of(), null, "", "", ""));
    }
    @Test
    void testInvalidEditsPreserveExistingProfile()
    {
        UserProfile profile = new UserProfile(1, "Alice", 20,
            Map.of("Running", "Beginner"));
        assertThrows(IllegalArgumentException.class, () -> profile.setName(null));
        assertThrows(IllegalArgumentException.class, () -> profile.setName("  "));
        assertThrows(IllegalArgumentException.class, () -> profile.setAge(0));
        assertThrows(IllegalArgumentException.class, () -> profile.setAge(-1));
        assertEquals("Alice", profile.getName());
        assertEquals(20, profile.getAge());
        profile.setName("Alex");
        profile.setAge(21);
        assertEquals("Alex", profile.getName());
        assertEquals(21, profile.getAge());
    }

    @Test
    void testInvalidWorkoutEditsPreserveInterests()
    {
        UserProfile profile = new UserProfile(1, "Alice", 20,
            Map.of("Running", "Beginner"));
        assertThrows(IllegalArgumentException.class,
            () -> profile.addWorkoutInterest(null, "Beginner"));
        assertThrows(IllegalArgumentException.class,
            () -> profile.addWorkoutInterest(" ", "Beginner"));
        assertThrows(IllegalArgumentException.class,
            () -> profile.addWorkoutInterest("Swimming", null));
        assertThrows(IllegalArgumentException.class,
            () -> profile.addWorkoutInterest("Running", " "));
        assertEquals(Map.of("Running", "Beginner"), profile.getWorkoutExperience());
        profile.addWorkoutInterest("Swimming", "Intermediate");
        assertEquals("Intermediate", profile.getWorkoutExperience().get("Swimming"));
    }

    @Test
    void testRequiredConstructorFieldsAndDefensiveCopies()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, null, 20, Map.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, "Alice", -1, Map.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new UserProfile(1, "Alice", 20, null));
        Map<String, String> interests = new java.util.HashMap<>();
        interests.put("Running", "Beginner");
        UserProfile profile = new UserProfile(1, "Alice", 20, interests);
        interests.clear();
        profile.getWorkoutExperience().clear();
        assertEquals(Map.of("Running", "Beginner"), profile.getWorkoutExperience());
    }

}

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
}

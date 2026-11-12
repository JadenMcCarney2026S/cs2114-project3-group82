package spotter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.HashMap;

class ProfileManagerTest
{

    @Test
    void testAddProfileNull()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20, 
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);
        
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addProfile(null);
        });
        assertTrue(manager.isEmpty());
    }
    
    @Test
    void testAddProfile()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);
        
        UserProfile grace = new UserProfile(
            1, "Grace", 20,
            new HashMap<>(java.util.Map.of("Running", "Intermediate")));
        
        manager.addProfile(grace);
        
        assertFalse(manager.isEmpty());
        assertEquals(grace, manager.getNextProfile());
        assertEquals("Grace", manager.getNextProfile().getName());
    }
    
    @Test
    void testRemoveNextProfile()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);
        
        UserProfile grace = new UserProfile(
            1, "Grace", 20,
            new HashMap<>(java.util.Map.of("Running", "Intermediate")));
        UserProfile phoebe = new UserProfile(
            2, "Phoebe", 21,
            new HashMap<>(java.util.Map.of("Running", "Advanced")));
        
        manager.addProfile(grace);
        manager.addProfile(phoebe);
        
        assertEquals(grace, manager.removeNextProfile());
        assertEquals(phoebe, manager.getNextProfile());
        assertEquals(phoebe, manager.removeNextProfile());
        assertTrue(manager.isEmpty());
    }
    
    @Test 
    void testRemoveNextProfileEmpty()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);
        
        assertNull(manager.removeNextProfile());
        assertNull(manager.getNextProfile());
        assertTrue(manager.isEmpty());
    }
    
    @Test
    void testDuplicateProfile()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);
        
        UserProfile grace = new UserProfile(
            1, "Grace", 20,
            new HashMap<>(java.util.Map.of("Running", "Intermediate")));
        
        manager.addProfile(grace);
        manager.addProfile(grace);
        
        assertEquals(grace, manager.removeNextProfile());
        assertTrue(manager.isEmpty());
        
        manager.addProfile(grace);
        assertTrue(manager.isEmpty());
    }
    
    @Test
    void testNoShareWorkoutInterest()
    {
        UserProfile currentUser = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));

        ProfileManager manager = new ProfileManager(currentUser);

        UserProfile grace = new UserProfile(
            1, "Grace", 20,
            new HashMap<>(java.util.Map.of("Swimming", "Intermediate")));

        manager.addProfile(grace);

        assertTrue(manager.isEmpty());
        assertNull(manager.getNextProfile());
    }
    
    @Test
    void testCannotAddCurrentUser()
    {
        UserProfile currentUser  = new UserProfile(
            0, "Current User", 20,
            new HashMap<>(java.util.Map.of("Running", "Beginner")));
        
        ProfileManager manager = new ProfileManager(currentUser);
        
        manager.addProfile(currentUser);
        
        assertTrue(manager.isEmpty());
        assertNull(manager.getNextProfile());
    }
}

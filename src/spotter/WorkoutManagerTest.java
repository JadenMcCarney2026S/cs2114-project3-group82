package spotter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class WorkoutManagerTest
{
    @Test
    public void testAddWorkout()
    {
        WorkoutManager manager = new WorkoutManager();
        Workout workout = new Workout("Push Day");
        
        manager.addWorkout(1, workout);
        assertEquals(workout, manager.getWorkout(1));
    }
    
    @Test
    public void testAddWorkoutNull()
    {
        WorkoutManager manager = new WorkoutManager();
        
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addWorkout(1, null);
        });
        
        assertNull(manager.getWorkout(1));
    }
    
    @Test
    public void testGetWorkoutNotFound()
    {
        WorkoutManager manager = new WorkoutManager();
        assertNull(manager.getWorkout(99));
    }
    
    @Test
    public void testReplaceWorkout()
    {
        WorkoutManager manager = new WorkoutManager();

        Workout first = new Workout("Push Day");
        Workout second = new Workout("Leg Day");

        manager.addWorkout(1, first);
        manager.addWorkout(1, second);

        assertEquals(second, manager.getWorkout(1));
        assertEquals("Leg Day", manager.getWorkout(1).getWorkoutName());
    }
    
    @Test
    public void testMultipleUsers()
    {
        WorkoutManager manager = new WorkoutManager();

        Workout first = new Workout("Push Day");
        Workout second = new Workout("Leg Day");

        manager.addWorkout(1, first);
        manager.addWorkout(2, second);

        assertSame(first, manager.getWorkout(1));
        assertSame(second, manager.getWorkout(2));
    }
}

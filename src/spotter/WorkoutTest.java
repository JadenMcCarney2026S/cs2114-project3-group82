package spotter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

public class WorkoutTest
{
    @Test
    public void testConstructor()
    {
        Workout workout = new Workout("Push Day");
        
        assertEquals("Push Day", workout.getWorkoutName());
        assertTrue(workout.getExercises().isEmpty());
    }
    
    @Test
    public void testConstructorInvalid()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            new Workout(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Workout("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Workout("   ");
        });
    }
    
    @Test
    public void testAddExercise()
    {
        Workout workout = new Workout("Push Day");
        
        workout.addExercise("Bench Press");
        workout.addExercise("Shoulder Press");
        
        assertEquals(2, workout.getExercises().size());
        assertEquals("Bench Press", workout.getExercises().get(0));
        assertEquals("Shoulder Press", workout.getExercises().get(1));
    }
    
    @Test
    public void testAddExerciseInvalid()
    {
        Workout workout = new Workout("Push Day");
        
        assertThrows(IllegalArgumentException.class, () -> {
            workout.addExercise(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            workout.addExercise("");
        });
        
        assertTrue(workout.getExercises().isEmpty());
    }
    
    @Test
    public void testGetExercises()
    {
        Workout workout = new Workout("Push Day");
        
        workout.addExercise("Bench Press");
        ArrayList<String> exercises = workout.getExercises();
        exercises.add("Squats");
        
        assertEquals(1, workout.getExercises().size());
        assertEquals("Bench Press", workout.getExercises().get(0));
    }
    
    @Test
    public void testGetWorkoutName()
    {
        Workout workout = new Workout("Leg Day");

        assertEquals("Leg Day", workout.getWorkoutName());

        workout.addExercise("Squats");

        assertEquals("Leg Day", workout.getWorkoutName());
    }
}

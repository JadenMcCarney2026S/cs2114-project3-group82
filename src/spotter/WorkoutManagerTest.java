package spotter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkoutManagerTest
{
    @Test
    void requiresAMatchManager()
    {
        assertThrows(IllegalArgumentException.class, () -> new WorkoutManager(null));
    }

    @Test
    void oneSidedLikeCannotReadOrChangeTheRoutine()
    {
        MatchManager matches = new MatchManager();
        WorkoutManager workouts = new WorkoutManager(matches);
        matches.recordSwipe(1, 2, SwipeAction.RIGHT);

        assertFalse(workouts.addWorkoutExercise(1, 2, "Push-up", 3, 10, 0));
        assertFalse(workouts.addWorkoutExercise(2, 1, "Push-up", 3, 10, 0));
        assertTrue(workouts.getWorkout(1, 2).isEmpty());
        assertTrue(workouts.getWorkout(2, 1).isEmpty());

        matches.recordSwipe(2, 1, SwipeAction.RIGHT);
        assertTrue(workouts.getWorkout(1, 2).isEmpty());
        assertTrue(workouts.addWorkoutExercise(1, 2, "Push-up", 3, 10, 0));
        assertEquals(List.of(new Workout.Exercise("Push-up", 3, 10, 0)),
            workouts.getWorkout(2, 1));
    }

    @Test
    void rejectsInvalidExercisesWithoutChangingTheRoutine()
    {
        MatchManager matches = new MatchManager();
        WorkoutManager workouts = new WorkoutManager(matches);
        matches.recordSwipe(1, 2, SwipeAction.RIGHT);
        matches.recordSwipe(2, 1, SwipeAction.RIGHT);
        assertTrue(workouts.addWorkoutExercise(1, 2, "Push-up", 3, 10, 0));
        assertFalse(workouts.addWorkoutExercise(1, 2, null, 3, 10, 0));
        assertFalse(workouts.addWorkoutExercise(1, 2, " ", 3, 10, 0));
        assertFalse(workouts.addWorkoutExercise(1, 2, "Row", 0, 10, 0));
        assertFalse(workouts.addWorkoutExercise(1, 2, "Row", 3, -1, 0));
        assertFalse(workouts.addWorkoutExercise(1, 2, "Row", 3, 10, -1));
        assertFalse(workouts.addWorkoutExercise(1, 2, "Row", 3, 10, Double.NaN));
        assertFalse(workouts.addWorkoutExercise(1, 2, "Row", 3, 10, Double.POSITIVE_INFINITY));
        assertEquals(List.of(new Workout.Exercise("Push-up", 3, 10, 0)),
            workouts.getWorkout(2, 1));
    }

    @Test
    void keepsPairsSeparateAndSnapshotsStable()
    {
        MatchManager matches = new MatchManager();
        WorkoutManager workouts = new WorkoutManager(matches);
        matches.recordSwipe(1, 2, SwipeAction.RIGHT);
        matches.recordSwipe(2, 1, SwipeAction.RIGHT);
        matches.recordSwipe(1, 3, SwipeAction.RIGHT);
        matches.recordSwipe(3, 1, SwipeAction.RIGHT);
        assertTrue(workouts.getWorkout(1, 2).isEmpty());
        assertTrue(workouts.addWorkoutExercise(1, 2, "Row", 3, 10, 15));
        var snapshot = workouts.getWorkout(1, 2);
        assertTrue(workouts.addWorkoutExercise(2, 1, "Squat", 3, 8, 25));
        assertEquals(1, snapshot.size());
        assertEquals(2, workouts.getWorkout(2, 1).size());
        assertTrue(workouts.getWorkout(1, 3).isEmpty());
        assertTrue(workouts.getWorkout(1, 1).isEmpty());
        assertFalse(workouts.addWorkoutExercise(1, 1, "Row", 3, 10, 15));
    }

    @Test
    void testAddWorkout()
    {
        WorkoutManager manager = new WorkoutManager();
        Workout workout = new Workout("Push Day");

        manager.addWorkout(1, workout);
        assertEquals(workout, manager.getWorkout(1));
    }

    @Test
    void testAddWorkoutNull()
    {
        WorkoutManager manager = new WorkoutManager();

        assertThrows(IllegalArgumentException.class, () -> {
            manager.addWorkout(1, null);
        });

        assertNull(manager.getWorkout(1));
    }

    @Test
    void testGetWorkoutNotFound()
    {
        WorkoutManager manager = new WorkoutManager();
        assertNull(manager.getWorkout(99));
    }

    @Test
    void testReplaceWorkout()
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
    void testMultipleUsers()
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

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
}

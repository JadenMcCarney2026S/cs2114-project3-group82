package spotter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores one workout per mutual match and checks access on every operation. */
public final class WorkoutManager
{
    private record Pair(int first, int second)
    {
        static Pair of(int first, int second)
        {
            return new Pair(Math.min(first, second), Math.max(first, second));
        }
    }

    private final MatchManager matches;
    private final Map<Pair, Workout> workouts = new HashMap<>();

    public WorkoutManager(MatchManager matches)
    {
        if (matches == null)
        {
            throw new IllegalArgumentException("Match manager cannot be null");
        }
        this.matches = matches;
    }

    boolean uses(MatchManager matchManager)
    {
        return matches == matchManager;
    }

    public boolean addWorkoutExercise(int userId, int otherId, String name, int sets,
        int reps, double weight)
    {
        if (!matches.areMatched(userId, otherId))
        {
            return false;
        }
        Workout.Exercise exercise;
        try
        {
            exercise = new Workout.Exercise(name, sets, reps, weight);
        }
        catch (IllegalArgumentException invalidExercise)
        {
            return false;
        }
        Pair pair = Pair.of(userId, otherId);
        Workout updated = workouts.getOrDefault(pair, Workout.EMPTY).addExercise(exercise);
        workouts.put(pair, updated);
        return true;
    }

    public List<Workout.Exercise> getWorkout(int userId, int otherId)
    {
        if (!matches.areMatched(userId, otherId))
        {
            return List.of();
        }
        return workouts.getOrDefault(Pair.of(userId, otherId), Workout.EMPTY).getExercises();
    }
}

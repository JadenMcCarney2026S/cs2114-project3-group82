package spotter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<Pair, Workout> sharedWorkouts = new HashMap<>();
    private final Map<Integer, Workout> personalWorkouts = new HashMap<>();

    public WorkoutManager()
    {
        matches = null;
    }

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
        if (matches == null || !matches.areMatched(userId, otherId))
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
        Workout updated = sharedWorkouts.getOrDefault(pair, Workout.EMPTY)
            .appendSharedExercise(exercise);
        sharedWorkouts.put(pair, updated);
        return true;
    }

    public List<Workout.Exercise> getWorkout(int userId, int otherId)
    {
        if (matches == null || !matches.areMatched(userId, otherId))
        {
            return List.of();
        }
        return sharedWorkouts.getOrDefault(Pair.of(userId, otherId), Workout.EMPTY)
            .getSharedExercises();
    }

    public void addWorkout(int userId, Workout workout)
    {
        if (workout == null)
        {
            throw new IllegalArgumentException("Workout cannot be null");
        }
        personalWorkouts.put(userId, workout);
    }

    public Workout getWorkout(int userId)
    {
        return personalWorkouts.get(userId);
    }
}

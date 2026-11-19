package spotter;

import java.util.ArrayList;
import java.util.List;

/** Immutable shared workout snapshot. */
public final class Workout
{
    public record Exercise(String name, int sets, int reps, double weight)
    {
        public Exercise
        {
            if (name == null || name.isBlank() || sets <= 0 || reps <= 0
                || !Double.isFinite(weight) || weight < 0)
            {
                throw new IllegalArgumentException(
                    "Use an exercise name, positive sets and reps, and a finite nonnegative weight.");
            }
            name = name.strip();
        }
    }

    static final Workout EMPTY = new Workout(List.of());

    private final List<Exercise> exercises;

    private Workout(List<Exercise> exercises)
    {
        this.exercises = List.copyOf(exercises);
    }

    Workout addExercise(Exercise exercise)
    {
        List<Exercise> updated = new ArrayList<>(exercises);
        updated.add(exercise);
        return new Workout(updated);
    }

    List<Exercise> getExercises()
    {
        return exercises;
    }
}

package spotter;

import java.util.ArrayList;
import java.util.List;

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

    private final String workoutName;
    private final ArrayList<String> exercises;
    private final List<Exercise> sharedExercises;

    public Workout(String workoutName)
    {
        if (workoutName == null || workoutName.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid workout name");
        }
        this.workoutName = workoutName;
        this.exercises = new ArrayList<>();
        sharedExercises = List.of();
    }

    private Workout(List<Exercise> exercises)
    {
        workoutName = "";
        this.exercises = new ArrayList<>();
        sharedExercises = List.copyOf(exercises);
    }

    Workout appendSharedExercise(Exercise exercise)
    {
        List<Exercise> updated = new ArrayList<>(sharedExercises);
        updated.add(exercise);
        return new Workout(updated);
    }

    List<Exercise> getSharedExercises()
    {
        return sharedExercises;
    }

    public void addExercise(String exercise)
    {
        if (exercise == null || exercise.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid exercise");
        }
        exercises.add(exercise);
    }

    public ArrayList<String> getExercises()
    {
        return new ArrayList<>(exercises);
    }

    public String getWorkoutName()
    {
        return workoutName;
    }
}

package spotter;

import java.util.ArrayList;

public class Workout
{
    private String workoutName;
    private ArrayList<String> exercises;
    
    public Workout(String workoutName)
    {
        if (workoutName == null || workoutName.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid workout name");
        }
        
        this.workoutName = workoutName;
        exercises = new ArrayList<>();
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

package spotter;

import java.util.HashMap;
import java.util.Map;

public class WorkoutManager
{
   private final Map<Integer, Workout> workouts;
   
   public WorkoutManager()
   {
       workouts = new HashMap<>();
   }
   
   public void addWorkout(int userId, Workout workout)
   {
       if (workout == null)
       {
           throw new IllegalArgumentException("Workout cannot be null");
       }

       workouts.put(userId, workout);
   }
   
   public Workout getWorkout(int userId)
   {
       return workouts.get(userId);
   }
}

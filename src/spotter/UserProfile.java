package spotter;

import java.util.HashMap;
import java.util.Map;

public class UserProfile
{
    private final int userId;
    private String name;
    private int age;
    private Map<String, String> workoutExperience;

    public UserProfile(
        int userId,
        String name,
        int age,
        Map<String, String> workoutExperience
        )
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid name");
        }
        
        if (age <= 0)
        {
            throw new IllegalArgumentException("Invalid age");
        }
        
        if (workoutExperience == null)
        {
            throw new IllegalArgumentException("Invalid workout experience");
        }
        
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.workoutExperience = new HashMap<>(workoutExperience);
    }
    
    public int getUserId()
    {
        return userId;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getAge()
    {
        return age;
    }
    
    public Map<String, String> getWorkoutExperience()
    {
        return new HashMap<>(workoutExperience);
    }
    
    public void setName(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid name");
        }
        this.name = name;
    }
    
    public void setAge(int age)
    {
        if (age <= 0)
        {
            throw new IllegalArgumentException("Invalid age");
        }
        this.age = age;
    }
    
    public void addWorkoutInterest(String activity, String level)
    {
        if (activity == null || activity.trim().isEmpty()
            || level == null || level.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid workout interest");
        }
        
        workoutExperience.put(activity, level);        
    }  
}
package spotter;

import java.util.HashMap;
import java.util.Map;

public class UserProfile
{
    private String goal = "";
    private String gym = "";
    private String preferredTime = "";
    private String bio = "";
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
    
    public UserProfile(int userId, String name, int age,
        Map<String, String> workoutExperience, String goal, String gym,
        String preferredTime, String bio)
    {
        this(userId, name, age, workoutExperience);
        if (goal == null || gym == null || preferredTime == null || bio == null)
        {
            throw new IllegalArgumentException("Profile details cannot be null");
        }
        this.goal = goal.trim();
        this.gym = gym.trim();
        this.preferredTime = preferredTime.trim();
        this.bio = bio.trim();
    }

    public String getGoal() { return goal; }
    public String getGym() { return gym; }
    public String getPreferredTime() { return preferredTime; }
    public String getBio() { return bio; }

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
package spotter;
import java.util.HashSet;
import java.util.Set;

public class ProfileManager
{
    private final LinkedQueue<UserProfile> profileQueue;
    private final Set<Integer> seenProfileIds;
    private final UserProfile currentUser;
    
    public ProfileManager(UserProfile currentUser)
    {
        if (currentUser == null)
        {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        this.currentUser = currentUser;
        profileQueue = new LinkedQueue<>();
        seenProfileIds = new HashSet<>();
    }
    
    public boolean addProfile(UserProfile profile)
    {
        if (profile == null)
        {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        
        if (profile.getUserId() == currentUser.getUserId()
            || seenProfileIds.contains(profile.getUserId()))
        {
            return false;
        }
        
        boolean sharedInterest = false;
        
        for (String activity : currentUser.getWorkoutExperience().keySet())
        {
            if (profile.getWorkoutExperience().containsKey(activity))
            {
                sharedInterest = true;
                break;
            }
        }

        if (!sharedInterest)
        {
            return false;
        }

        profileQueue.enqueue(profile);
        seenProfileIds.add(profile.getUserId());
        return true;
     }
    
    public UserProfile getNextProfile()
    {
        if (profileQueue.isEmpty())
        {
            return null;
        }
        
        return profileQueue.peek();
    }
    
    public UserProfile removeNextProfile()
    {
        if (profileQueue.isEmpty())
        {
            return null;
        }
        return profileQueue.dequeue();
    }
    
    public boolean isEmpty()
    {
        return profileQueue.isEmpty();
    }
}

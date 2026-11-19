package spotter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainLogic
{
    private final UserProfile currentUser;
    private final ProfileManager profileManager;
    private final MatchManager matchManager;
    private final ChatManager chatManager;
    private final WorkoutManager workoutManager;
    private final Map<Integer, UserProfile> profiles = new LinkedHashMap<>();

    public MainLogic(UserProfile currentUser)
    {
        this(currentUser, new ChatManager(new MatchManager()));
    }

    /** Pass the same ChatManager to user sessions to share matches and messages. */
    public MainLogic(UserProfile currentUser, ChatManager chatManager)
    {
        if (chatManager == null)
        {
            throw new IllegalArgumentException("Chat manager cannot be null");
        }
        profileManager = new ProfileManager(currentUser);
        this.currentUser = currentUser;
        this.chatManager = chatManager;
        matchManager = chatManager.getMatchManager();
        workoutManager = new WorkoutManager();
    }

    UserProfile getCurrentUser()
    {
        return currentUser;
    }

    ChatManager getChatManager()
    {
        return chatManager;
    }
    
    public boolean createWorkout(String workoutName)
    {
        try
        {
            Workout workout = new Workout(workoutName);
            workoutManager.addWorkout(currentUser.getUserId(), workout);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }
    
    public Workout getWorkout()
    {
        return workoutManager.getWorkout(currentUser.getUserId());
    }
    

    public boolean addExercise(String exercise)
    {
        Workout workout = getWorkout();
    
        if (workout == null)
        {
            return false;
        }
    
        try
        {
            workout.addExercise(exercise);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    public boolean addProfile(UserProfile profile)
    {
        if (profile == null || profile.getUserId() == currentUser.getUserId()
            || profiles.containsKey(profile.getUserId())
            || matchManager.hasSwiped(currentUser.getUserId(), profile.getUserId()))
        {
            return false;
        }
        if (!profileManager.addProfile(profile))
        {
            return false;
        }
        profiles.put(profile.getUserId(), profile);
        return true;
    }

    public UserProfile getNextProfile()
    {
        return profileManager.getNextProfile();
    }

    /** Null means no new match, including a valid swipe without a match. */
    public UserProfile swipeCurrentProfile(SwipeAction action)
    {
        UserProfile profile = getNextProfile();
        if (action == null || profile == null)
        {
            return null;
        }
        boolean newMatch = matchManager.recordSwipe(currentUser.getUserId(),
            profile.getUserId(), action);
        profileManager.removeNextProfile();
        return newMatch ? profile : null;
    }

    public List<UserProfile> getMatches()
    {
        List<UserProfile> result = new ArrayList<>();
        for (UserProfile profile : profiles.values())
        {
            if (matchManager.areMatched(currentUser.getUserId(), profile.getUserId()))
            {
                result.add(profile);
            }
        }
        return result;
    }

    public boolean sendMessage(int recipientId, String content)
    {
        return chatManager.sendMessage(currentUser.getUserId(), recipientId, content);
    }

    public List<String> getConversation(int otherUserId)
    {
        return chatManager.getConversation(currentUser.getUserId(), otherUserId);
    }
}

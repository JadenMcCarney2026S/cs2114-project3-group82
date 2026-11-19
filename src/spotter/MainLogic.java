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
        this(currentUser, chatManager, chatManager == null ? null
            : new WorkoutManager(chatManager.getMatchManager()));
    }

    MainLogic(UserProfile currentUser, ChatManager chatManager, WorkoutManager workoutManager)
    {
        if (chatManager == null)
        {
            throw new IllegalArgumentException("Chat manager cannot be null");
        }
        if (workoutManager == null || !workoutManager.uses(chatManager.getMatchManager()))
        {
            throw new IllegalArgumentException("Workout and chat must use the same match manager");
        }
        profileManager = new ProfileManager(currentUser);
        this.currentUser = currentUser;
        this.chatManager = chatManager;
        this.workoutManager = workoutManager;
        matchManager = chatManager.getMatchManager();
    }

    UserProfile getCurrentUser()
    {
        return currentUser;
    }

    ChatManager getChatManager()
    {
        return chatManager;
    }

    WorkoutManager getWorkoutManager()
    {
        return workoutManager;
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

    boolean canViewProfile(int otherUserId)
    {
        return profiles.containsKey(otherUserId);
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

    public boolean addWorkoutExercise(int otherUserId, String name, int sets,
        int reps, double weight)
    {
        return workoutManager.addWorkoutExercise(currentUser.getUserId(), otherUserId,
            name, sets, reps, weight);
    }

    public List<Workout.Exercise> getWorkout(int otherUserId)
    {
        return workoutManager.getWorkout(currentUser.getUserId(), otherUserId);
    }
}

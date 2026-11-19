package spotter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SpotterController
{
    private final Map<Integer, MainLogic> sessions;
    private final SpotterView view;
    private MainLogic model;
    private int currentUserId;

    public SpotterController(Map<Integer, MainLogic> sessions, int currentUserId,
        SpotterView view)
    {
        if (sessions == null || !sessions.containsKey(currentUserId)
            || sessions.values().stream().anyMatch(java.util.Objects::isNull) || view == null)
        {
            throw new IllegalArgumentException("Valid sessions and view are required");
        }
        this.sessions = new LinkedHashMap<>(sessions);
        this.model = sessions.get(currentUserId);
        this.currentUserId = currentUserId;
        this.view = view;
    }

    /** Register an in-memory demo identity and offer eligible profiles both ways. */
    public boolean addDemoUser(UserProfile user)
    {
        if (user == null || sessions.containsKey(user.getUserId()))
        {
            return false;
        }
        MainLogic newSession = new MainLogic(user, model.getChatManager());
        for (MainLogic session : sessions.values())
        {
            newSession.addProfile(session.getCurrentUser());
            session.addProfile(user);
        }
        sessions.put(user.getUserId(), newSession);
        return true;
    }

    public UserProfile displayNextProfile()
    {
        UserProfile profile = model.getNextProfile();
        view.showProfile(profile);
        return profile;
    }

    /** Returns whether a new match was created, not whether a swipe was accepted. */
    public boolean handleSwipe(SwipeAction action)
    {
        if (action == null)
        {
            view.showError("Choose left or right.");
            return false;
        }
        if (model.getNextProfile() == null)
        {
            displayNextProfile();
            return false;
        }
        UserProfile match = model.swipeCurrentProfile(action);
        view.showInfo(match == null ? "Swipe recorded."
            : "New match: " + match.getName());
        displayNextProfile();
        return match != null;
    }

    public List<UserProfile> displayMatches()
    {
        List<UserProfile> matches = model.getMatches();
        view.showMatches(matches);
        return matches;
    }

    public boolean sendChatMessage(int recipientId, String content)
    {
        boolean sent = model.sendMessage(recipientId, content);
        if (sent)
        {
            view.showInfo("Message sent.");
        }
        else
        {
            view.showError("Message rejected: use a match ID and nonempty text.");
        }
        return sent;
    }

    public List<String> displayConversation(int otherUserId)
    {
        List<String> conversation = model.getConversation(otherUserId);
        view.showChat(conversation);
        return conversation;
    }
    

    public boolean createWorkout(String workoutName)
    {
        boolean created = model.createWorkout(workoutName);
    
        if (created)
        {
            view.showInfo("Workout created: " + workoutName);
        }
        else
        {
            view.showError("Invalid workout name.");
        }
    
        return created;
    }
    
    public boolean addWorkoutExercise(String exercise)
    {
        boolean added = model.addExercise(exercise);
    
        if (added)
        {
            view.showInfo("Exercise added: " + exercise);
        }
        else
        {
            view.showError("Cannot add exercise.");
        }
    
        return added;
    }
    
    public Workout displayWorkout()
    {
        Workout workout = model.getWorkout();
    
        if (workout == null)
        {
            view.showInfo("No workout created.");
        }
        else
        {
            view.showInfo("Workout: " + workout.getWorkoutName());
    
            for (String exercise : workout.getExercises())
            {
                view.showInfo("- " + exercise);
            }
        }
    
        return workout;
    }


    public void run(Scanner input)
    {
        view.showMenu();
        displayNextProfile();
        while (true)
        {
            view.showPrompt(currentUserId);
            if (!input.hasNextLine())
            {
                break;
            }
            if (!handleCommand(input.nextLine()))
            {
                return;
            }
        }
        view.showInfo("Goodbye.");
    }

    public boolean handleCommand(String line)
    {
        if (line == null)
        {
            view.showError("Enter a command.");
            return true;
        }
        String[] parts = line.trim().split("\\s+", 3);
        try
        {
            switch (parts[0])
            {
                case "profile":
                case "left":
                case "right":
                case "matches":
                case "help":
                case "quit":
                    if (parts.length != 1)
                    {
                        view.showError("Unexpected arguments.");
                        break;
                    }
                    switch (parts[0])
                    {
                        case "profile": displayNextProfile(); break;
                        case "left": handleSwipe(SwipeAction.LEFT); break;
                        case "right": handleSwipe(SwipeAction.RIGHT); break;
                        case "matches": displayMatches(); break;
                        case "help":
                            view.showMenu();
                            break;
                        case "quit": view.showInfo("Goodbye."); return false;
                        default: break;
                    }
                    break;
                case "send":
                    if (parts.length < 2)
                    {
                        view.showError("Usage: send <id> <text>");
                        break;
                    }
                    sendChatMessage(Integer.parseInt(parts[1]),
                        parts.length == 3 ? parts[2] : "");
                    break;
                case "chat":
                case "user":
                    if (parts.length != 2)
                    {
                        view.showError("Usage: " + parts[0] + " <id>");
                        break;
                    }
                    int id = Integer.parseInt(parts[1]);
                    if (parts[0].equals("chat"))
                    {
                        displayConversation(id);
                    }
                    else if (sessions.containsKey(id))
                    {
                        model = sessions.get(id);
                        currentUserId = id;
                        view.showInfo("Active user: " + id);
                        displayNextProfile();
                    }
    else
    {
        view.showError("Unknown user ID.");
    }
    break;
    
    case "workout":
        if (parts.length != 1)
        {
            view.showError("Usage: workout");
            break;
        }
        displayWorkout();
        break;
    
    case "createworkout":
        if (parts.length < 2)
        {
            view.showError("Usage: createworkout <name>");
            break;
        }
        createWorkout(line.trim().substring(parts[0].length()).trim());
        break;
    
    case "addexercise":
        if (parts.length < 2)
        {
            view.showError("Usage: addexercise <exercise>");
            break;
        }
        addWorkoutExercise(line.trim().substring(parts[0].length()).trim());
        break;
    
    default:
        view.showError("Unknown command. Type help.");
        break;

            }
        }
        catch (NumberFormatException exception)
        {
            view.showError("User ID must be a 32-bit integer.");
        }
        return true;
    }
}

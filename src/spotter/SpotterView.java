package spotter;

import java.io.PrintStream;
import java.util.List;

/** Console formatting only; all actions go through the controller. */
public class SpotterView
{
    private final WorkoutView workoutView = new WorkoutView();
    private final PrintStream output;
    private final java.util.Scanner input;

    public SpotterView(PrintStream output)
    {
        this(System.in, output);
    }

    public SpotterView()
    {
        this(System.in, System.out);
    }

    public SpotterView(java.io.InputStream input, PrintStream output)
    {
        this.input = new java.util.Scanner(input);
        this.output = output;
    }

    public void showProfile(String name, int age, String gender,
        String workout, String experience, String location)
    {
        ProfileView formatter = new ProfileView();
        output.println(formatter.format(name, age, gender, workout, experience, location));
        output.println(formatter.formatSwipeOptions());
    }

    public void showNoProfiles()
    {
        output.println(new ProfileView().formatEmpty());
    }

    public void showMatches(java.util.Collection<String> names)
    {
        output.println(new MatchesView().format(new java.util.ArrayList<>(names)));
    }

    public void showNewMatch(String name)
    {
        output.println(new MatchesView().formatNewMatch(name));
    }

    public void showChat(String otherName, List<String> messages)
    {
        output.println(new ChatView().format(otherName, messages));
    }

    public String readLine(String prompt)
    {
        output.print(prompt);
        output.flush();
        return input.hasNextLine() ? input.nextLine().trim() : null;
    }

    public String readChoice()
    {
        String line = readLine("> ");
        return line == null ? null : line.toUpperCase(java.util.Locale.ROOT);
    }

    public String readMessage()
    {
        return readLine(new ChatView().formatPrompt());
    }

    public void showProfile(UserProfile profile)
    {
        if (profile == null)
        {
            showInfo("No more profiles.");
            return;
        }
        showInfo("\n--- NEXT PROFILE ---");
        showInfo("Profile " + profile.getUserId() + ": " + profile.getName()
            + ", age " + profile.getAge() + ", " + profile.getWorkoutExperience());
        showInfo("Goal: " + profile.getGoal() + " | Gym: " + profile.getGym());
        showInfo("Time: " + profile.getPreferredTime() + " | About: " + profile.getBio());
    }

    public void showMatches(List<UserProfile> matches)
    {
        showInfo("\n--- YOUR MATCHES (" + matches.size() + ") ---");
        if (matches.isEmpty())
        {
            showInfo("No matches yet.");
        }
        for (UserProfile profile : matches)
        {
            showInfo("Match " + profile.getUserId() + ": " + profile.getName());
        }
    }

    public void showChat(List<String> conversation)
    {
        showInfo("\n--- CONVERSATION ---");
        if (conversation.isEmpty())
        {
            showInfo("No accessible messages.");
        }
        for (String message : conversation)
        {
            showInfo(message);
        }
    }

    public void showMenu()
    {
        showInfo("\n=============== SPOTTER ===============");
        showInfo("profile           View the next candidate");
        showInfo("left / right      Pass / like, then advance the queue");
        showInfo("matches           View mutual matches");
        showInfo("send <id> <text>  Message a match");
        showInfo("chat <id>         Read your conversation");
        showInfo("user <id>         Switch demo user (not login)");
        showInfo("createworkout <name>  Create a new workout");
        showInfo("addexercise <name>   Add an exercise to your workout");
        showInfo("workout              View your current workout");
        showInfo("help / quit       Show this menu / exit");
        showInfo("=======================================");
    }

    public void showPrompt(int userId)
    {
        output.print("\nspotter [user " + userId + "]> ");
        output.flush();
    }

    public void showInfo(String message)
    {
        output.println(message);
    }

    public void showError(String message)
    {
        output.println("Error: " + message);
    }
    
    public void showWorkout(List<String> exerciseLines)
    {
        output.println(workoutView.format(exerciseLines));
    }


    public void showWorkout(String workoutName, List<String> exerciseLines)
    {
        output.println(workoutView.format(workoutName, exerciseLines));
    }


}

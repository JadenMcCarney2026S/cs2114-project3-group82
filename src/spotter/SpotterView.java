package spotter;

import java.io.PrintStream;
import java.util.List;

/** Console formatting only; all actions go through the controller. */
public class SpotterView
{
    private final PrintStream output;

    public SpotterView(PrintStream output)
    {
        this.output = output;
    }

    public void showProfile(UserProfile profile)
    {
        if (profile == null)
        {
            showInfo("No more profiles.");
            return;
        }
        showInfo("Profile " + profile.getUserId() + ": " + profile.getName()
            + ", age " + profile.getAge() + ", " + profile.getWorkoutExperience());
    }

    public void showMatches(List<UserProfile> matches)
    {
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
        if (conversation.isEmpty())
        {
            showInfo("No accessible messages.");
        }
        for (String message : conversation)
        {
            showInfo(message);
        }
    }

    public void showInfo(String message)
    {
        output.println(message);
    }

    public void showError(String message)
    {
        output.println("Error: " + message);
    }
}

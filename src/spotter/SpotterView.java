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
        showInfo("\n--- NEXT PROFILE ---");
        showInfo("Profile " + profile.getUserId() + ": " + profile.getName()
            + ", age " + profile.getAge() + ", " + profile.getWorkoutExperience());
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
}

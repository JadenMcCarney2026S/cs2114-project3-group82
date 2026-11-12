package spotter;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

// -------------------------------------------------------------------------
/**
 * SpotterView Class, it is the main interface
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class SpotterView
{
    private final Scanner in;
    private final PrintStream out;
    private final ProfileView profileView;
    private final MatchesView matchesView;
    private final ChatView chatView;

    /**
     * Console version: System.in / System.out.
     */
    public SpotterView()
    {
        this(System.in, System.out);
    }


    /**
     * Injectable streams so the view can be tested without a real console.
     */
    public SpotterView(InputStream input, PrintStream output)
    {
        this.in = new Scanner(input);
        this.out = output;
        this.profileView = new ProfileView();
        this.matchesView = new MatchesView();
        this.chatView = new ChatView();
    }


    public void showProfile(
        String name,
        int age,
        String gender,
        String workout,
        String gymExperience,
        String location)
    {
        out.println(
            profileView
                .format(name, age, gender, workout, gymExperience, location));
        out.println(profileView.formatSwipeOptions());
    }


    /**
     * Shown when the queue is empty.
     */
    public void showNoProfiles()
    {
        out.println(profileView.formatEmpty());
    }


    public void showMatches(List<String> matchedNames)
    {
        out.println(matchesView.format(matchedNames));
    }


    public void showNewMatch(String name)
    {
        out.println(matchesView.formatNewMatch(name));
    }


    public void showChat(String otherName, List<String> messages)
    {
        out.println(chatView.format(otherName, messages));
    }


    public void showInfo(String message)
    {
        out.println("[info] " + message);
    }


    public void showError(String message)
    {
        out.println("[error] " + message);
    }


    /**
     * Prints a prompt and returns the trimmed line, or null if input has ended.
     */
    public String readLine(String prompt)
    {
        out.print(prompt);
        out.flush();
        if (!in.hasNextLine())
        {
            return null;
        }
        return in.nextLine().trim();
    }


    /**
     * Reads a menu choice, upper-cased (e.g. "L", "R", "M", "Q").
     */
    public String readChoice()
    {
        String line = readLine("> ");
        return line == null ? null : line.toUpperCase();
    }


    /**
     * Reads the text of a chat message.
     */
    public String readMessage()
    {
        return readLine(chatView.formatPrompt());
    }

}

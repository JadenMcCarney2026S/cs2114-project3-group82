package spotter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// -------------------------------------------------------------------------
/**
 * SpotterView Test Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class SpotterViewTest
{

    private ByteArrayOutputStream captured;

    /**
     * setUp
     */
    public void setUp()
    {
        captured = new ByteArrayOutputStream();
    }


    /**
     * Builds a view that reads from the given text and writes to the captured
     * stream.
     * 
     * @param input
     *            the fake user input
     * @return the view
     */
    private SpotterView viewWithInput(String input)
    {
        return new SpotterView(
            new ByteArrayInputStream(input.getBytes()),
            new PrintStream(captured));
    }


    /**
     * test show profile
     */
    public void testShowProfile()
    {
        SpotterView view = viewWithInput("");
        view.showProfile(
            "Alex",
            20,
            "Male",
            "Upper/Lower",
            "Beginner",
            "Campus Gym");
        String out = captured.toString();
        assertTrue(out.contains("Alex, 20"));
        assertTrue(out.contains("Campus Gym"));
        assertTrue(out.contains("Swipe right"));
    }


    /**
     * test show no profiles
     */
    public void testShowNoProfiles()
    {
        SpotterView view = viewWithInput("");
        view.showNoProfiles();
        assertTrue(captured.toString().contains("No more profiles"));
    }


    /**
     * test show matches
     */
    public void testShowMatches()
    {
        SpotterView view = viewWithInput("");
        List<String> names = new ArrayList<String>();
        names.add("Sam");
        names.add("Priya");
        view.showMatches(names);
        String out = captured.toString();
        assertTrue(out.contains("Sam"));
        assertTrue(out.contains("Priya"));
    }


    /**
     * test show new match
     */
    public void testShowNewMatch()
    {
        SpotterView view = viewWithInput("");
        view.showNewMatch("Jordan");
        assertTrue(captured.toString().contains("Jordan"));
    }


    /**
     * test show chat
     */
    public void testShowChat()
    {
        SpotterView view = viewWithInput("");
        List<String> messages = new ArrayList<String>();
        messages.add("hello");
        view.showChat("Sam", messages);
        String out = captured.toString();
        assertTrue(out.contains("Chat with Sam"));
        assertTrue(out.contains("hello"));
    }


    /**
     * test show info and show error
     */
    public void testShowInfoAndError()
    {
        SpotterView view = viewWithInput("");
        view.showInfo("Swipe recorded");
        view.showError("Message cannot be empty");
        String out = captured.toString();
        assertTrue(out.contains("[info] Swipe recorded"));
        assertTrue(out.contains("[error] Message cannot be empty"));
    }


    /**
     * test read choice trims and upper-cases
     */
    public void testReadChoice()
    {
        SpotterView view = viewWithInput("  r  \n");
        assertEquals("R", view.readChoice());
    }


    /**
     * test read choice when input ends
     */
    public void testReadChoiceEndOfInput()
    {
        SpotterView view = viewWithInput("");
        assertNull(view.readChoice());
    }


    /**
     * test read message
     */
    public void testReadMessage()
    {
        SpotterView view = viewWithInput("see you at 6\n");
        assertEquals("see you at 6", view.readMessage());
    }


    /**
     * test read message when user presses enter
     */
    public void testReadMessageBlank()
    {
        SpotterView view = viewWithInput("\n");
        assertEquals("", view.readMessage());
    }


    /**
     * test read message when input ends
     */
    public void testReadMessageEndOfInput()
    {
        SpotterView view = viewWithInput("");
        assertNull(view.readMessage());
    }


    /**
     * test several reads in order
     */
    public void testMultipleReads()
    {
        SpotterView view = viewWithInput("l\nr\nm\n");
        assertEquals("L", view.readChoice());
        assertEquals("R", view.readChoice());
        assertEquals("M", view.readChoice());
        assertNull(view.readChoice());
    }
}

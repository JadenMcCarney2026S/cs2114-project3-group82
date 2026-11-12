package spotter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// -------------------------------------------------------------------------
/**
 * ChatView Test Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class ChatViewTest
{
    private ChatView view;
    private List<String> messages;

    /**
     * setUp
     */
    public void setUp()
    {
        view = new ChatView();
        messages = new ArrayList<String>();
        messages.add("Hey!");
        messages.add("Leg day tomorrow?");
    }


    /**
     * test format shows name and messages in order
     */
    public void testFormatMessages()
    {
        String text = view.format("Sam", messages);
        assertTrue(text.contains("Chat with Sam"));
        assertTrue(text.indexOf("Hey!") < text.indexOf("Leg day tomorrow?"));
    }


    /**
     * test format empty conversation
     */
    public void testFormatEmpty()
    {
        String text = view.format("Sam", new ArrayList<String>());
        assertTrue(text.contains("no messages yet"));
    }


    /**
     * test format null messages
     */
    public void testFormatNullMessages()
    {
        String text = view.format("Sam", null);
        assertTrue(text.contains("no messages yet"));
    }


    /**
     * test format null name
     */
    public void testFormatNullName()
    {
        String text = view.format(null, messages);
        assertFalse(text.contains("null"));
    }


    /**
     * test prompt
     */
    public void testFormatPrompt()
    {
        assertTrue(view.formatPrompt().contains("Enter"));
    }

}

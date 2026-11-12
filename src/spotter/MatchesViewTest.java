package spotter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// -------------------------------------------------------------------------
/**
 * MatchesView Test Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class MatchesViewTest
{
    private MatchesView view;
    private List<String> names;

    /**
     * setUp
     */
    public void setUp()
    {
        view = new MatchesView();
        names = new ArrayList<String>();
        names.add("Sam");
        names.add("Priya");
    }


    /**
     * test format lists matches in order
     */
    public void testFormatList()
    {
        String text = view.format(names);
        assertTrue(text.contains("(2)"));
        assertTrue(text.contains("1. Sam"));
        assertTrue(text.contains("2. Priya"));
        assertTrue(text.indexOf("Sam") < text.indexOf("Priya"));
    }


    /**
     * test format empty list
     */
    public void testFormatEmpty()
    {
        assertTrue(
            view.format(new ArrayList<String>()).contains("no matches yet"));
    }


    /**
     * test format null list
     */
    public void testFormatNull()
    {
        assertTrue(view.format(null).contains("no matches yet"));
    }


    /**
     * test new match banner
     */
    public void testFormatNewMatch()
    {
        String banner = view.formatNewMatch("Jordan");
        assertTrue(banner.contains("match"));
        assertTrue(banner.contains("Jordan"));
    }
}

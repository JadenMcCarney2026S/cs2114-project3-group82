package spotter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// -------------------------------------------------------------------------
/**
 * ProflieView Test Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class ProfileViewTest
{

    private ProfileView view;

    /**
     * setUp
     */
    @BeforeEach
    public void setUp()
    {
        view = new ProfileView();
    }


    /**
     * test format includes all fields
     */
    @Test
    public void testFormatAllFields()
    {
        String card = view.format(
            "Alex",
            20,
            "Male",
            "Push/Pull/Legs",
            "Intermediate",
            "Campus Gym");
        assertTrue(card.contains("Alex, 20"));
        assertTrue(card.contains("Male"));
        assertTrue(card.contains("Push/Pull/Legs"));
        assertTrue(card.contains("Intermediate"));
        assertTrue(card.contains("Campus Gym"));
    }


    /**
     * test format with null and empty fields
     */
    @Test
    public void testFormatMissingFields()
    {
        String card = view.format(null, 20, "", null, "", null);
        assertTrue(card.contains("N/A"));
        assertFalse(card.contains("null"));
    }


    /**
     * test swipe options
     */
    @Test
    public void testFormatSwipeOptions()
    {
        String options = view.formatSwipeOptions();
        assertTrue(options.contains("Swipe left"));
        assertTrue(options.contains("Swipe right"));
        assertTrue(options.contains("Matches"));
        assertTrue(options.contains("Quit"));
    }


    /**
     * test empty message
     */
    @Test
    public void testFormatEmpty()
    {
        assertTrue(view.formatEmpty().contains("No more profiles"));
    }
}

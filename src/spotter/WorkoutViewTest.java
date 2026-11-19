package spotter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// -------------------------------------------------------------------------
/**
 * Workout View Class Test
 *
 * @author varoonisawant
 * @version Nov 19, 2026
 */
public class WorkoutViewTest
{
    private WorkoutView view;

    /**
     * setUp
     */

    void setUp()
    {
        view = new WorkoutView();
    }


    /**
     * test numbered lines in order
     */

    void testFormatNumbersExercisesInOrder()
    {
        String text =
            view.format(Arrays.asList("Squat 3x10 135lb", "Bench 4x8 95lb"));
        assertTrue(text.contains("Shared workout routine:"));
        assertTrue(text.contains("1. Squat 3x10 135lb"));
        assertTrue(text.contains("2. Bench 4x8 95lb"));
        assertTrue(text.indexOf("Squat") < text.indexOf("Bench"));
    }


    /**
     * test empty list
     */

    void testFormatEmptyList()
    {
        assertEquals("No exercises yet.", view.format(new ArrayList<String>()));
    }


    /**
     * test null list
     */

    void testFormatNullList()
    {
        assertEquals("No exercises yet.", view.format(null));
    }


    /**
     * test null and blank entries are skipped without crashing
     */

    void testFormatSkipsNullAndBlankEntries()
    {
        String text = view.format(Arrays.asList(null, "  ", "Row 3x12 50lb"));
        assertTrue(text.contains("1. Row 3x12 50lb"));
        assertFalse(text.contains("null"));
    }


    /**
     * test list with only bad entries
     */

    void testFormatOnlyBadEntries()
    {
        assertEquals("No exercises yet.", view.format(Arrays.asList(null, "")));
    }


    /**
     * test the view does not change the list it is given
     */

    void testFormatDoesNotModifyInput()
    {
        List<String> lines = new ArrayList<String>();
        lines.add("Squat 3x10 135lb");
        view.format(lines);
        assertEquals(1, lines.size());
        assertEquals("Squat 3x10 135lb", lines.get(0));
    }


    /**
     * test the name appears in the header
     */

    void testFormatWithNameShowsNameInHeader()
    {
        String text = view.format("Leg Day", Arrays.asList("Squat 3x10"));
        assertTrue(text.contains("Shared workout routine (Leg Day):"));
        assertTrue(text.contains("1. Squat 3x10"));
    }


    /**
     * test null and blank names fall back to the default header
     */

    void testFormatWithNullOrBlankNameUsesDefaultHeader()
    {
        String nullName = view.format(null, Arrays.asList("Squat 3x10"));
        String blankName = view.format("   ", Arrays.asList("Squat 3x10"));
        assertTrue(nullName.contains("Shared workout routine:"));
        assertTrue(blankName.contains("Shared workout routine:"));
    }


    /**
     * test a name with no exercises still shows the empty message
     */

    void testFormatWithNameAndNoExercises()
    {
        assertEquals(
            "No exercises yet.",
            view.format("Leg Day", new ArrayList<String>()));
    }
}

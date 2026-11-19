package spotter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// -------------------------------------------------------------------------
/**
 * Tests SpotterView.showWorkout.
 *
 * @author varoonisawant
 * @version Nov 19, 2026
 */
public class SpotterViewWorkoutTest
{
    private ByteArrayOutputStream output;
    private SpotterView view;

    /**
     * setUp
     */

    void setUp()
    {
        output = new ByteArrayOutputStream();
        view = new SpotterView(new PrintStream(output));
    }


    /**
     * test show workout prints the routine
     */

    void testShowWorkoutPrintsRoutine()
    {
        view.showWorkout(Arrays.asList("Squat 3x10 135lb"));
        assertTrue(output.toString().contains("Shared workout routine:"));
        assertTrue(output.toString().contains("1. Squat 3x10 135lb"));
    }


    /**
     * test show workout with a name prints the name
     */

    void testShowWorkoutWithName()
    {
        view.showWorkout("Leg Day", Arrays.asList("Squat 3x10 135lb"));
        assertTrue(output.toString().contains("(Leg Day)"));
    }


    /**
     * test show workout with an empty routine
     */

    void testShowWorkoutEmpty()
    {
        view.showWorkout(new ArrayList<String>());
        assertTrue(output.toString().contains("No exercises yet."));
    }


    /**
     * test show workout with null does not crash
     */
    void testShowWorkoutNull()
    {
        view.showWorkout(null);
        assertTrue(output.toString().contains("No exercises yet."));
    }
}

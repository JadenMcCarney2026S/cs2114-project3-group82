package spotter;

import java.util.List;

// -------------------------------------------------------------------------
/**
 * Workout View Class
 * 
 * @author varoonisawant
 * @version Nov 19, 2026
 */
public class WorkoutView
{
    private static final String EMPTY = "No exercises yet.";
    private static final String HEADER = "Shared workout routine";

    /**
     * Builds the text for a routine.
     *
     * @param exerciseLines
     *            one display line per exercise, in routine order; may be null
     * @return the text to show, never null
     */
    public String format(List<String> exerciseLines)
    {
        return format(null, exerciseLines);
    }


    /**
     * Builds the text for a named routine.
     *
     * @param workoutName
     *            the routine name; null or blank means no name is shown
     * @param exerciseLines
     *            one display line per exercise, in routine order; may be null
     * @return the text to show, never null
     */
    public String format(String workoutName, List<String> exerciseLines)
    {
        if (exerciseLines == null)
        {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (String line : exerciseLines)
        {
            if (line == null || line.trim().isEmpty())
            {
                continue;
            }
            sb.append("  ").append(number).append(". ").append(line)
                .append("\n");
            number++;
        }
        if (number == 1)
        {
            return EMPTY;
        }
        String title = HEADER;
        if (workoutName != null && !workoutName.trim().isEmpty())
        {
            title = HEADER + " (" + workoutName.trim() + ")";
        }
        return title + ":\n" + sb.toString().stripTrailing();
    }
}

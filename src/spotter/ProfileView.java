package spotter;

// -------------------------------------------------------------------------
/**
 * Profile View class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class ProfileView
{
    /**
     * Builds the text for one profile card.
     */
    public String format(
        String name,
        int age,
        String gender,
        String workout,
        String gymExperience,
        String location)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("+----------------------------------------+\n");
        sb.append("  ").append(safe(name)).append(", ").append(age)
            .append("\n");
        sb.append("+----------------------------------------+\n");
        sb.append("  Gender:         ").append(safe(gender)).append("\n");
        sb.append("  Workout split:  ").append(safe(workout)).append("\n");
        sb.append("  Gym experience: ").append(safe(gymExperience))
            .append("\n");
        sb.append("  Location:       ").append(safe(location)).append("\n");
        sb.append("+----------------------------------------+\n");
        return sb.toString();
    }

    /**
     * Text shown under every profile card.
     */
    public String formatSwipeOptions()
    {
        return "[L] Swipe left (pass)   [R] Swipe right (like)   [M] Matches   [Q] Quit";
    }

    /**
     * Text shown when the queue has no more profiles.
     */
    public String formatEmpty()
    {
        return "No more profiles are available right now. Check back later!";
    }

    private String safe(String value)
    {
        return (value == null || value.isEmpty()) ? "N/A" : value;
    }
}

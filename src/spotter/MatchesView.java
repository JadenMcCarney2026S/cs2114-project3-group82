package spotter;

import java.util.List;

// -------------------------------------------------------------------------
/**
 * MatchesView Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class MatchesView
{
    /**
     * Builds the numbered list of matched names.
     */
    public String format(List<String> matchedNames)
    {
        if (matchedNames == null || matchedNames.isEmpty())
        {
            return "You have no matches yet. Keep swiping!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== Your Matches (").append(matchedNames.size())
            .append(") ===\n");
        for (int i = 0; i < matchedNames.size(); i++)
        {
            sb.append("  ").append(i + 1).append(". ")
                .append(matchedNames.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Banner shown right after a swipe creates a mutual match.
     */
    public String formatNewMatch(String name)
    {
        return "*** It's a match! You and " + name + " can now chat. ***";
    }
}

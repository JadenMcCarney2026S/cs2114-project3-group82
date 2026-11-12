package spotter;

import java.util.List;

// -------------------------------------------------------------------------
/**
 * CHatView Class
 * 
 * @author varoonisawant
 * @version Nov 12, 2026
 */
public class ChatView
{
    /**
     * Builds the conversation text.
     */
    public String format(String otherName, List<String> messages)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Chat with ").append(otherName == null ? "?" : otherName)
            .append(" ===\n");
        if (messages == null || messages.isEmpty())
        {
            sb.append("  (no messages yet - say hi!)\n");
        }
        else
        {
            for (String line : messages)
            {
                sb.append("  ").append(line).append("\n");
            }
        }
        sb.append("==========================================\n");
        return sb.toString();
    }


    /**
     * Prompt shown when asking the user to type a message.
     */
    public String formatPrompt()
    {
        return "Type a message (or press Enter to go back): ";
    }
}

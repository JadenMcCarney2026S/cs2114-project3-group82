package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatManagerTest
{
    @Test
    void boundsHistoryAndResetsStaleCursors()
    {
        MatchManager matches = new MatchManager();
        ChatManager chat = new ChatManager(matches, 2);
        matches.recordSwipe(1, 2, SwipeAction.RIGHT);
        matches.recordSwipe(2, 1, SwipeAction.RIGHT);

        assertTrue(chat.sendMessage(1, 2, "First"));
        assertTrue(chat.sendMessage(2, 1, "Second"));
        assertTrue(chat.sendMessage(1, 2, "Third"));
        assertFalse(chat.sendMessage(3, 2, "Unmatched"));

        var retained = new ChatManager.History(1, 3, List.of("2: Second", "1: Third"));
        assertEquals(retained, chat.getHistory(2, 1, 0));
        assertEquals(retained, chat.getHistory(2, 1, 9));
        assertEquals(new ChatManager.History(2, 3, List.of("1: Third")),
            chat.getHistory(2, 1, 2));
        assertEquals(new ChatManager.History(0, 0, List.of()), chat.getHistory(3, 1, 0));
    }
}

package spotter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SpotterLanViewTest
{
    @Test
    void separatesDraftsAndRetriesOnlyTheSameSend()
    {
        SpotterLanView.Drafts drafts = new SpotterLanView.Drafts();
        String firstId = drafts.idForSend(2, "For Bob");
        assertEquals("", drafts.text(3));
        drafts.save(3, "For Carol");
        assertEquals("For Bob", drafts.text(2));
        assertEquals(firstId, drafts.idForSend(2, "For Bob"));
        assertNotEquals(firstId, drafts.idForSend(3, "For Carol"));
        assertTrue(drafts.complete(2, firstId));
        assertNotEquals(firstId, drafts.idForSend(2, "For Bob"));
        assertEquals("For Carol", drafts.text(3));
    }

    @Test
    void oldSendCompletionPreservesEditedDraft()
    {
        SpotterLanView.Drafts drafts = new SpotterLanView.Drafts();
        String sentId = drafts.idForSend(2, "Original");
        drafts.save(2, "Edited");
        assertFalse(drafts.complete(2, sentId));
        assertEquals("Edited", drafts.text(2));
        assertNotEquals(sentId, drafts.idForSend(2, "Edited"));
    }
}

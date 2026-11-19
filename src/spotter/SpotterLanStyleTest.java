package spotter;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.junit.jupiter.api.Test;

class SpotterLanStyleTest
{
    @Test
    void primaryButtonPaintsItsAccentBackground()
    {
        javax.swing.JButton button = new javax.swing.JButton("Sign up");
        SpotterLanStyle.button(button, true);
        assertTrue(button.isOpaque());
        assertTrue(button.isContentAreaFilled());
        assertEquals(SpotterLanStyle.ACCENT, button.getBackground());
    }

    @Test
    void alertShowsLiteralErrorAndNextStep()
    {
        JPanel content = SpotterLanStyle.alertContent("<html>Invalid input", "Use 12–128 characters.");
        JTextArea detail = (JTextArea)content.getComponent(0);
        JTextArea guidance = (JTextArea)content.getComponent(1);
        assertEquals("<html>Invalid input", detail.getText());
        assertEquals("Use 12–128 characters.", guidance.getText());
        assertTrue(detail.getLineWrap());
        assertFalse(guidance.isEditable());
    }
}

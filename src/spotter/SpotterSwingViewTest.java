package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class SpotterSwingViewTest
{
    private Component find(Container root, String name)
    {
        for (Component child : root.getComponents())
        {
            if (name.equals(child.getName()))
            {
                return child;
            }
            if (child instanceof Container)
            {
                Component result = find((Container)child, name);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }

    @Test
    void testButtonsCreateRealMatchAndSharedConversation() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new SpotterSwingView().getPanel();
            JButton like = (JButton)find(panel, "like");
            JComboBox<?> users = (JComboBox<?>)find(panel, "users");
            JList<?> matches = (JList<?>)find(panel, "matches");
            JTextField message = (JTextField)find(panel, "message");
            JButton send = (JButton)find(panel, "send");
            JTextArea history = (JTextArea)find(panel, "conversation");
            like.doClick();
            assertEquals(0, matches.getModel().getSize());
            assertFalse(send.isEnabled());
            users.setSelectedIndex(1);
            like.doClick();
            assertEquals(1, matches.getModel().getSize());
            matches.setSelectedIndex(0);
            message.setText("Hello Alice");
            send.doClick();
            assertTrue(history.getText().contains("2: Hello Alice"));
            message.setText("   ");
            send.doClick();
            assertTrue(((JLabel)find(panel, "status")).getText().contains("rejected"));
            users.setSelectedIndex(0);
            assertEquals(1, matches.getModel().getSize());
            matches.setSelectedIndex(0);
            message.setText("Hi Bob");
            send.doClick();
            assertTrue(history.getText().contains("2: Hello Alice"));
            assertTrue(history.getText().contains("1: Hi Bob"));
            users.setSelectedIndex(2);
            assertEquals(0, matches.getModel().getSize());
            assertFalse(send.isEnabled());
            assertFalse(history.getText().contains("Hello Alice"));
            assertFalse(like.isEnabled());
        });
    }

    @Test
    void testPassingAndExhaustedQueue() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new SpotterSwingView().getPanel();
            JButton pass = (JButton)find(panel, "pass");
            JButton like = (JButton)find(panel, "like");
            pass.doClick();
            assertTrue(pass.isEnabled());
            pass.doClick();
            assertFalse(pass.isEnabled());
            assertFalse(like.isEnabled());
            ((JComboBox<?>)find(panel, "users")).setSelectedIndex(1);
            like.doClick();
            assertEquals(0, ((JList<?>)find(panel, "matches")).getModel().getSize());
        });
    }
    @Test
    void testCreateProfileAndMatchWithExistingUser() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView view = new SpotterSwingView();
            JPanel panel = view.getPanel();
            UserProfile user = new UserProfile(5, "Eve", 24,
                Map.of("Running", "Beginner"), "Build stamina", "Campus gym",
                "Evening", "Looking for a running buddy.");
            assertTrue(view.createDemoProfile(user));
            assertFalse(view.createDemoProfile(user));
            JComboBox<?> users = (JComboBox<?>)find(panel, "users");
            assertEquals(5, users.getItemCount());
            assertEquals(4, users.getSelectedIndex());
            JButton like = (JButton)find(panel, "like");
            like.doClick(); // Eve likes Alice.
            users.setSelectedIndex(0);
            JButton pass = (JButton)find(panel, "pass");
            pass.doClick(); // Bob
            pass.doClick(); // Dave
            assertTrue(((JTextArea)find(panel, "details")).getText().contains("Build stamina"));
            like.doClick(); // Alice likes Eve.
            JList<?> matches = (JList<?>)find(panel, "matches");
            assertEquals(1, matches.getModel().getSize());
            matches.setSelectedIndex(0);
            ((JTextField)find(panel, "message")).setText("Welcome Eve");
            ((JButton)find(panel, "send")).doClick();
            users.setSelectedIndex(4);
            matches.setSelectedIndex(0);
            assertTrue(((JTextArea)find(panel, "conversation")).getText().contains("Welcome Eve"));
        });
    }

}

package spotter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
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
    void testSwipesCoverTheCardWithoutAllowingAClickThrough()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView view = new SpotterSwingView();
            JPanel panel = view.getPanel();
            panel.setSize(panel.getPreferredSize());
            layout(panel);
            Component card = find(panel, "discoveryCard");
            assertNotNull(card);

            ((JButton)find(panel, "like")).doClick(0);
            assertTrue(((JTextArea)find(panel, "details")).getText().contains("Swimming"));
            assertTrue(coversCardWithColor(card, 0xC7FC7B));

            JButton pass = (JButton)find(panel, "pass");
            assertFalse(pass.isEnabled());
            pass.doClick(0);
            assertTrue(((JTextArea)find(panel, "details")).getText().contains("Swimming"));
            waitForSwipeControls(pass);
            pass.doClick(0);
            assertFalse(((JTextArea)find(panel, "details")).getText().contains("Swimming"));
            assertTrue(coversCardWithColor(card, 0xFFC49B));
            assertFalse(coversCardWithColor(card, 0xC7FC7B));
            view.close();
        });
    }

    private static void waitForSwipeControls(JButton action)
    {
        var loop = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        long deadline = System.nanoTime() + 3_000_000_000L;
        Timer check = new Timer(25, event -> {
            if (action.isEnabled() || System.nanoTime() >= deadline)
            {
                loop.exit();
            }
        });
        check.start();
        try
        {
            loop.enter();
        }
        finally
        {
            check.stop();
        }
        assertTrue(action.isEnabled(), "Swipe controls did not return");
    }

    private static boolean coversCardWithColor(Component card, int rgb)
    {
        BufferedImage image = new BufferedImage(card.getWidth(), card.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try
        {
            card.paint(graphics);
        }
        finally
        {
            graphics.dispose();
        }
        return (image.getRGB(16, 16) & 0xFFFFFF) == rgb
            && (image.getRGB(card.getWidth() - 16, card.getHeight() - 16) & 0xFFFFFF) == rgb;
    }

    @Test
    void testDiscoveryMessagesAndWorkoutUseSeparateTabs() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView view = new SpotterSwingView();
            JTabbedPane sections = (JTabbedPane)find(view.getPanel(), "sections");
            assertNotNull(sections);
            assertEquals(3, sections.getTabCount());
            assertEquals("Discovery", sections.getTitleAt(0));
            assertEquals("Messages", sections.getTitleAt(1));
            assertEquals("Shared workout", sections.getTitleAt(2));
            assertNotNull(find((Container)sections.getComponentAt(0), "like"));
            assertNotNull(find((Container)sections.getComponentAt(1), "conversation"));
            Container messages = (Container)sections.getComponentAt(1);
            Container routine = (Container)sections.getComponentAt(2);
            Component matchList = find(messages, "matches");
            assertNotNull(matchList);
            sections.setSelectedIndex(2);
            assertSame(matchList, find(routine, "matches"));
            view.getPanel().setSize(view.getPanel().getPreferredSize());
            layout(view.getPanel());
            assertTrue(find(routine, "workout").getWidth() >= 600);
            sections.setSelectedIndex(1);
            assertSame(matchList, find(messages, "matches"));
            view.close();
        });
    }

    private static void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents())
        {
            if (child instanceof Container container)
            {
                layout(container);
            }
        }
    }

    @Test
    void testSharedWorkoutReflectsExerciseAcrossMatchedViews() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView alice = new SpotterSwingView();
            SpotterSwingView bob = alice.openPeerView(2);
            JPanel alicePanel = alice.getPanel();
            JPanel bobPanel = bob.getPanel();
            JButton add = (JButton)find(alicePanel, "addExercise");
            assertFalse(add.isEnabled());
            ((JButton)find(alicePanel, "like")).doClick();
            ((JButton)find(bobPanel, "like")).doClick();
            ((JList<?>)find(alicePanel, "matches")).setSelectedIndex(0);
            ((JList<?>)find(bobPanel, "matches")).setSelectedIndex(0);
            assertTrue(add.isEnabled());
            ((JTextField)find(alicePanel, "exerciseName")).setText("Push ups");
            ((JTextField)find(alicePanel, "exerciseSets")).setText("zero");
            add.doClick();
            assertTrue(((JLabel)find(alicePanel, "status")).getText()
                .contains("Sets and reps must be whole numbers"));
            assertFalse(((JTextArea)find(bobPanel, "workout")).getText().contains("Push ups"));
            ((JTextField)find(alicePanel, "exerciseSets")).setText("3");
            ((JTextField)find(alicePanel, "exerciseReps")).setText("8");
            ((JTextField)find(alicePanel, "exerciseWeight")).setText("0");
            add.doClick();
            assertTrue(((JTextArea)find(alicePanel, "workout")).getText().contains("Push ups"));
            assertTrue(((JTextArea)find(bobPanel, "workout")).getText().contains("Push ups"));
            ((JComboBox<?>)find(bobPanel, "users")).setSelectedIndex(2);
            assertFalse(((JTextArea)find(bobPanel, "workout")).getText().contains("Push ups"));
            alice.close();
            bob.close();
        });
    }

    @Test
    void testLiveChatBetweenIndependentViews() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Map<Integer, MainLogic> sessions = SpotterApp.createSessions();
            SpotterSwingView alice = new SpotterSwingView(sessions, 1);
            SpotterSwingView bob = alice.openPeerView(2);
            JPanel alicePanel = alice.getPanel();
            JPanel bobPanel = bob.getPanel();
            ((JButton)find(alicePanel, "like")).doClick();
            ((JButton)find(bobPanel, "like")).doClick();
            ((JList<?>)find(alicePanel, "matches")).setSelectedIndex(0);
            ((JList<?>)find(bobPanel, "matches")).setSelectedIndex(0);
            JTextArea aliceHistory = (JTextArea)find(alicePanel, "conversation");
            JTextArea bobHistory = (JTextArea)find(bobPanel, "conversation");
            send(alicePanel, "Hi Bob");
            assertEquals("Alice: Hi Bob", bobHistory.getText());
            send(bobPanel, "Hi Alice: see you at 6");
            assertEquals("Alice: Hi Bob\n\nBob: Hi Alice: see you at 6", aliceHistory.getText());
            send(bobPanel, "   ");
            assertEquals(aliceHistory.getText(), bobHistory.getText());
            ((JComboBox<?>)find(bobPanel, "users")).setSelectedIndex(2);
            assertTrue(sessions.get(1).sendMessage(2, "Private"));
            assertTrue(aliceHistory.getText().contains("Private"));
            assertFalse(bobHistory.getText().contains("Private"));
            ((JComboBox<?>)find(bobPanel, "users")).setSelectedIndex(1);
            ((JList<?>)find(bobPanel, "matches")).setSelectedIndex(0);
            assertTrue(bobHistory.getText().contains("Private"));
            bob.close();
            send(alicePanel, "After close");
            assertFalse(bobHistory.getText().contains("After close"));
            alice.close();
        });
    }

    private void send(JPanel panel, String content)
    {
        ((JTextField)find(panel, "message")).setText(content);
        ((JButton)find(panel, "send")).doClick();
    }

    @Test
    void testNewProfileAppearsInPeerAccountPicker() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView alice = new SpotterSwingView();
            SpotterSwingView bob = alice.openPeerView(2);
            assertTrue(alice.createDemoProfile(new UserProfile(5, "Eve", 24,
                Map.of("Running", "Beginner"))));
            JComboBox<?> bobUsers = (JComboBox<?>)find(bob.getPanel(), "users");
            assertEquals("Bob / 2", bobUsers.getSelectedItem());
            assertEquals("Eve / 5", bobUsers.getItemAt(4));
            bobUsers.setSelectedIndex(4);
            assertEquals("Active user: 5",
                ((JLabel)find(bob.getPanel(), "status")).getText());
            bob.close();
            alice.close();
        });
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
            assertTrue(history.getText().contains("Bob: Hello Alice"));
            message.setText("   ");
            send.doClick();
            assertTrue(((JLabel)find(panel, "status")).getText().contains("rejected"));
            users.setSelectedIndex(0);
            assertEquals(1, matches.getModel().getSize());
            matches.setSelectedIndex(0);
            message.setText("Hi Bob");
            send.doClick();
            assertTrue(history.getText().contains("Bob: Hello Alice"));
            assertTrue(history.getText().contains("Alice: Hi Bob"));
            users.setSelectedIndex(2);
            assertEquals(0, matches.getModel().getSize());
            assertFalse(send.isEnabled());
            assertFalse(history.getText().contains("Hello Alice"));
            assertTrue(like.isEnabled());
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
            waitForSwipeControls(pass);
            assertTrue(pass.isEnabled());
            assertTrue(((JTextArea)find(panel, "details")).getText().contains("Swimming"));
            assertNotNull(((JLabel)find(panel, "avatar")).getIcon());
            pass.doClick();
            waitForSwipeControls(pass);
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
            waitForSwipeControls(pass);
            pass.doClick(); // Carol
            waitForSwipeControls(pass);
            pass.doClick(); // Dave
            waitForSwipeControls(pass);
            assertTrue(((JTextArea)find(panel, "details")).getText().contains("Build stamina"));
            like.doClick(); // Alice likes Eve.
            JList<?> matches = (JList<?>)find(panel, "matches");
            assertEquals(1, matches.getModel().getSize());
            matches.setSelectedIndex(0);
            ((JTextField)find(panel, "message")).setText("Welcome Eve");
            ((JButton)find(panel, "send")).doClick();
            users.setSelectedIndex(4);
            matches.setSelectedIndex(0);
            assertEquals("Alice: Welcome Eve", ((JTextArea)find(panel, "conversation")).getText());
        });
    }

    @Test
    void testDemoPhotosAndFallback() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpotterSwingView view = new SpotterSwingView();
            JLabel avatar = (JLabel)find(view.getPanel(), "avatar");
            for (int id = 1; id <= 4; id++)
            {
                view.showProfile(new UserProfile(id, "Demo", 20, Map.of()));
                assertNotNull(avatar.getIcon(), "Missing demo photo for " + id);
                assertEquals(348, avatar.getIcon().getIconWidth());
                assertEquals(420, avatar.getIcon().getIconHeight());
                assertEquals("", avatar.getText());
            }
            view.showProfile(new UserProfile(99, "Eve", 20, Map.of()));
            assertNull(avatar.getIcon());
            assertEquals("E", avatar.getText());
            view.showProfile(null);
            assertNull(avatar.getIcon());
            assertEquals("-", avatar.getText());
        });
    }

}

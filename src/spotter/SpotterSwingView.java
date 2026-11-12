package spotter;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Optional desktop presentation; business rules stay in the existing model. */
public class SpotterSwingView extends SpotterView
{
    private static final Color INK = new Color(225, 239, 230);
    private static final Color ACCENT = new Color(77, 230, 142);
    private static final Color BACKGROUND = new Color(9, 13, 11);
    private static final Color SURFACE = new Color(20, 28, 23);
    private static final Color MUTED = new Color(149, 174, 158);
    private final JPanel panel = new JPanel(new BorderLayout(24, 24));
    private final JLabel name = new JLabel("Discover", SwingConstants.CENTER);
    private final JTextArea details = new JTextArea();
    private final JLabel avatar = new JLabel("S", SwingConstants.CENTER);
    private final JLabel status = new JLabel("Like someone to get started.");
    private final JButton pass = button("Pass", "pass");
    private final JButton like = button("Like", "like");
    private final DefaultListModel<UserProfile> matchItems = new DefaultListModel<>();
    private final JList<UserProfile> matches = new JList<>(matchItems);
    private final JTextArea conversation = new JTextArea();
    private final JTextField message = new JTextField();
    private final JButton send = button("Send", "send");
    private final JComboBox<String> users = new JComboBox<>(new String[] {
        "Alice / 1", "Bob / 2", "Carol / 3", "Dave / 4" });
    private final SpotterController controller;

    public SpotterSwingView()
    {
        super(System.out);
        controller = new SpotterController(SpotterApp.createSessions(), 1, this);
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));
        panel.setPreferredSize(new Dimension(960, 650));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel brand = new JLabel("spotter.");
        brand.setFont(new Font("SansSerif", Font.BOLD, 34));
        brand.setForeground(ACCENT);
        header.add(brand, BorderLayout.WEST);
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        account.setOpaque(false);
        JLabel accountLabel = new JLabel("Demo account");
        accountLabel.setForeground(MUTED);
        account.add(accountLabel);
        users.setName("users");
        users.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        users.setBackground(SURFACE);
        users.setForeground(INK);
        account.add(users);
        JButton create = button("Create profile", "create");
        create.setPreferredSize(new Dimension(170, 48));
        create.addActionListener(event -> showCreateProfile());
        account.add(create);
        header.add(account, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(SURFACE);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel intro = new JLabel("FIND YOUR NEXT GYM PARTNER", SwingConstants.CENTER);
        intro.setForeground(MUTED);
        card.add(intro, BorderLayout.NORTH);
        JPanel identity = new JPanel(new GridLayout(3, 1, 0, 12));
        identity.setOpaque(false);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(22, 55, 36));
        avatar.setForeground(ACCENT);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 68));
        name.setFont(new Font("SansSerif", Font.BOLD, 30));
        name.setForeground(INK);
        identity.add(avatar);
        identity.add(name);
        details.setForeground(MUTED);
        details.setName("details");
        details.setBackground(SURFACE);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        identity.add(details);
        card.add(identity, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 16, 0));
        actions.setOpaque(false);
        actions.add(pass);
        actions.add(like);
        card.add(actions, BorderLayout.SOUTH);

        JPanel inbox = new JPanel(new BorderLayout(10, 12));
        inbox.setBackground(SURFACE);
        inbox.setBorder(new EmptyBorder(20, 20, 20, 20));
        inbox.setPreferredSize(new Dimension(340, 480));
        JLabel title = new JLabel("Your matches & messages");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(INK);
        inbox.add(title, BorderLayout.NORTH);
        matches.setName("matches");
        matches.setBackground(BACKGROUND);
        matches.setForeground(INK);
        matches.setSelectionBackground(new Color(30, 75, 46));
        matches.setSelectionForeground(ACCENT);
        matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matches.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel)super.getListCellRendererComponent(
                    list, value, index, selected, focus);
                UserProfile profile = (UserProfile)value;
                label.setText(profile.getName() + "  /  " + profile.getUserId());
                label.setBorder(new EmptyBorder(9, 10, 9, 10));
                return label;
            }
        });
        conversation.setName("conversation");
        conversation.setBackground(BACKGROUND);
        conversation.setForeground(INK);
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(new Font("SansSerif", Font.PLAIN, 15));
        conversation.setMargin(new Insets(12, 12, 12, 12));
        JSplitPane chats = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(matches), new JScrollPane(conversation));
        chats.setDividerLocation(140);
        chats.setResizeWeight(0.35);
        chats.setBorder(BorderFactory.createLineBorder(SURFACE));
        chats.setBackground(SURFACE);
        chats.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI());
        inbox.add(chats, BorderLayout.CENTER);
        JPanel compose = new JPanel(new BorderLayout(8, 0));
        compose.setOpaque(false);
        message.setName("message");
        message.setBackground(BACKGROUND);
        message.setForeground(INK);
        message.setCaretColor(ACCENT);
        message.setDisabledTextColor(MUTED);
        message.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(49, 75, 59)),
            new EmptyBorder(8, 10, 8, 10)));
        compose.add(message, BorderLayout.CENTER);
        compose.add(send, BorderLayout.EAST);
        inbox.add(compose, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);
        body.add(card, BorderLayout.CENTER);
        body.add(inbox, BorderLayout.EAST);
        panel.add(body, BorderLayout.CENTER);
        status.setName("status");
        status.setBorder(new EmptyBorder(8, 0, 0, 0));
        panel.add(status, BorderLayout.SOUTH);

        pass.addActionListener(event -> swipe(SwipeAction.LEFT));
        like.addActionListener(event -> swipe(SwipeAction.RIGHT));
        users.addActionListener(event -> {
            matches.clearSelection();
            conversation.setText("");
            message.setText("");
            String selectedUser = (String)users.getSelectedItem();
            controller.handleCommand("user " + selectedUser.substring(selectedUser.lastIndexOf(" / ") + 3));
            controller.displayMatches();
            updateComposer();
        });
        matches.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting())
            {
                UserProfile selected = matches.getSelectedValue();
                if (selected != null)
                {
                    controller.displayConversation(selected.getUserId());
                }
                updateComposer();
            }
        });
        send.addActionListener(event -> sendMessage());
        message.addActionListener(event -> sendMessage());
        controller.displayNextProfile();
        controller.displayMatches();
        updateComposer();
        showInfo("Alice likes Bob? Switch to Bob and like Alice to create a real match.");
    }

    private void showCreateProfile()
    {
        JTextField[] fields = new JTextField[6];
        String[] labels = { "Name", "Age", "Goal (optional)", "Gym (optional)",
            "Preferred time (optional)", "About me (optional)" };
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < 6; i++)
        {
            fields[i] = new JTextField();
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        JComboBox<String> activity = new JComboBox<>(new String[] {
            "Running", "Swimming", "Strength training", "Cycling" });
        JComboBox<String> level = new JComboBox<>(new String[] {
            "Beginner", "Intermediate", "Advanced" });
        form.add(new JLabel("Workout interest"));
        form.add(activity);
        form.add(new JLabel("Experience"));
        form.add(level);
        while (JOptionPane.showConfirmDialog(panel, form, "Create demo profile",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
        {
            try
            {
                UserProfile user = new UserProfile(users.getItemCount() + 1,
                    fields[0].getText(), Integer.parseInt(fields[1].getText().trim()),
                    java.util.Map.of((String)activity.getSelectedItem(),
                        (String)level.getSelectedItem()), fields[2].getText(),
                    fields[3].getText(), fields[4].getText(), fields[5].getText());
                if (createDemoProfile(user))
                {
                    return;
                }
            }
            catch (IllegalArgumentException exception)
            {
                JOptionPane.showMessageDialog(panel,
                    "Enter a name and a positive whole-number age.",
                    "Check profile", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean createDemoProfile(UserProfile user)
    {
        if (!controller.addDemoUser(user))
        {
            showError("Profile ID already exists or profile is missing.");
            return false;
        }
        users.addItem(user.getName() + " / " + user.getUserId());
        users.setSelectedIndex(users.getItemCount() - 1);
        controller.handleCommand("user " + user.getUserId());
        controller.displayMatches();
        showInfo("Demo profile created. Data is kept until this app closes.");
        return true;
    }

    private static JButton button(String text, String componentName)
    {
        JButton result = new JButton(text);
        result.setName(componentName);
        result.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        result.setOpaque(true);
        result.setBackground(componentName.equals("pass") ? SURFACE : ACCENT);
        result.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(49, 75, 59)),
            new EmptyBorder(8, 12, 8, 12)));
        result.setFont(new Font("SansSerif", Font.BOLD, 16));
        result.setForeground(componentName.equals("pass") ? INK : BACKGROUND);
        result.setPreferredSize(new Dimension(100, 48));
        return result;
    }

    private void swipe(SwipeAction action)
    {
        controller.handleSwipe(action);
        controller.displayMatches();
    }

    private void updateComposer()
    {
        boolean selected = matches.getSelectedValue() != null;
        message.setEnabled(selected);
        send.setEnabled(selected);
        message.setToolTipText(selected ? "Write a message" : "Select a mutual match first");
    }

    private void sendMessage()
    {
        UserProfile selected = matches.getSelectedValue();
        if (selected != null && controller.sendChatMessage(
            selected.getUserId(), message.getText()))
        {
            message.setText("");
            controller.displayConversation(selected.getUserId());
        }
    }

    @Override
    public void showProfile(UserProfile profile)
    {
        boolean available = profile != null;
        pass.setEnabled(available);
        like.setEnabled(available);
        avatar.setText(available ? profile.getName().substring(0, 1).toUpperCase() : "-");
        name.setText(available ? profile.getName() + ", " + profile.getAge() : "All caught up");
        details.setText(available ? "Interests: " + profile.getWorkoutExperience()
            + "\nGoal: " + profile.getGoal() + "\nGym: " + profile.getGym()
            + "\nTime: " + profile.getPreferredTime() + "\nAbout: " + profile.getBio()
            : "No eligible profiles left. Try another demo account.");
    }

    @Override
    public void showMatches(List<UserProfile> profiles)
    {
        matchItems.clear();
        for (UserProfile profile : profiles)
        {
            matchItems.addElement(profile);
        }
        conversation.setText(profiles.isEmpty()
            ? "No matches yet. A match needs two likes." : "Select a match to chat.");
        updateComposer();
    }

    @Override
    public void showChat(List<String> messages)
    {
        conversation.setText(messages.isEmpty() ? "Say hello to your gym partner."
            : String.join("\n\n", messages));
    }

    @Override
    public void showInfo(String text)
    {
        status.setForeground(INK);
        status.setText(text);
    }

    @Override
    public void showError(String text)
    {
        status.setForeground(ACCENT);
        status.setText(text);
    }

    public JPanel getPanel()
    {
        return panel;
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            SpotterSwingView view = new SpotterSwingView();
            JFrame window = new JFrame("Spotter - Gym partners");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(view.getPanel());
            window.pack();
            window.setMinimumSize(new Dimension(820, 600));
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}

package spotter;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Map;
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
    private final JTextArea workout = new JTextArea();
    private final JTextField exerciseName = new JTextField();
    private final JTextField exerciseSets = new JTextField();
    private final JTextField exerciseReps = new JTextField();
    private final JTextField exerciseWeight = new JTextField();
    private final JButton addExercise = button("Add exercise", "addExercise");
    private final JTextField message = new JTextField();
    private final JButton send = button("Send", "send");
    private final JComboBox<String> users = new JComboBox<>();
    private final Map<Integer, MainLogic> sessions;
    private final PropertyChangeSupport updates;
    private final PropertyChangeListener updateListener = this::refreshPeer;
    private final PropertyChangeListener messageListener = this::refreshConversation;
    private final ChatManager chat;
    private final SpotterController controller;
    private boolean closed;

    public SpotterSwingView()
    {
        this(SpotterApp.createSessions(), 1);
    }

    SpotterSwingView(Map<Integer, MainLogic> sessions, int userId)
    {
        this(sessions, userId, new PropertyChangeSupport(sessions));
    }

    private SpotterSwingView(Map<Integer, MainLogic> sessions, int userId,
        PropertyChangeSupport updates)
    {
        super(System.out);
        this.sessions = sessions;
        this.updates = updates;
        chat = sessions.get(userId).getChatManager();
        controller = new SpotterController(sessions, userId, this, true);
        sessions.values().forEach(session -> addAccount(session.getCurrentUser()));
        users.setSelectedItem(accountLabel(sessions.get(userId).getCurrentUser()));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));
        panel.setPreferredSize(new Dimension(960, 820));

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
        JPanel identity = new JPanel(new BorderLayout(0, 12));
        identity.setOpaque(false);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(22, 55, 36));
        avatar.setForeground(ACCENT);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 68));
        name.setFont(new Font("SansSerif", Font.BOLD, 30));
        name.setForeground(INK);
        avatar.setName("avatar");
        identity.add(avatar, BorderLayout.CENTER);
        JPanel profileInfo = new JPanel(new BorderLayout(0, 8));
        profileInfo.setOpaque(false);
        profileInfo.add(name, BorderLayout.NORTH);
        details.setForeground(MUTED);
        details.setName("details");
        details.setBackground(SURFACE);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.setPreferredSize(new Dimension(400, 115));
        profileInfo.add(details, BorderLayout.CENTER);
        identity.add(profileInfo, BorderLayout.SOUTH);
        card.add(identity, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 16, 0));
        actions.setOpaque(false);
        actions.add(pass);
        actions.add(like);
        card.add(actions, BorderLayout.SOUTH);

        matches.setName("matches");
        styleMatches(matches);
        conversation.setName("conversation");
        conversation.setBackground(BACKGROUND);
        conversation.setForeground(INK);
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(new Font("SansSerif", Font.PLAIN, 15));
        conversation.setMargin(new Insets(12, 12, 12, 12));
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
        JPanel messages = new JPanel(new BorderLayout(0, 10));
        messages.setOpaque(false);
        messages.add(new JScrollPane(conversation), BorderLayout.CENTER);
        messages.add(compose, BorderLayout.SOUTH);

        JTabbedPane sections = new JTabbedPane();
        sections.setName("sections");
        sections.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI()
        {
            @Override
            protected void paintTabBackground(Graphics graphics, int placement, int index,
                int x, int y, int width, int height, boolean selected)
            {
                graphics.setColor(selected ? new Color(30, 75, 46) : SURFACE);
                graphics.fillRect(x, y, width, height);
            }
        });
        sections.setBackground(BACKGROUND);
        sections.setForeground(INK);
        sections.setFont(new Font("SansSerif", Font.BOLD, 15));
        JPanel sidebar = matchSidebar(matches);
        JPanel messagesWorkspace = matchWorkspace(messages);
        JPanel workoutWorkspace = matchWorkspace(workoutPanel());
        messagesWorkspace.add(sidebar, BorderLayout.WEST);
        sections.addTab("Discovery", card);
        sections.addTab("Messages", messagesWorkspace);
        sections.addTab("Shared workout", workoutWorkspace);
        sections.addChangeListener(event -> {
            if (sections.getSelectedComponent() != card)
            {
                JPanel workspace = (JPanel)sections.getSelectedComponent();
                workspace.add(sidebar, BorderLayout.WEST);
                workspace.revalidate();
                workspace.repaint();
            }
        });
        panel.add(sections, BorderLayout.CENTER);
        status.setName("status");
        status.setBorder(new EmptyBorder(8, 0, 0, 0));
        panel.add(status, BorderLayout.SOUTH);

        pass.addActionListener(event -> swipe(SwipeAction.LEFT));
        like.addActionListener(event -> swipe(SwipeAction.RIGHT));
        users.addActionListener(event -> {
            matches.clearSelection();
            conversation.setText("");
            clearExerciseFields();
            message.setText("");
            String selectedUser = (String)users.getSelectedItem();
            controller.handleCommand("user " + selectedUser.substring(selectedUser.lastIndexOf(" / ") + 3));
            controller.displayMatches();
            updateComposer();
        });
        matches.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting())
            {
                clearExerciseFields();
                UserProfile selected = matches.getSelectedValue();
                if (selected != null)
                {
                    controller.displayConversation(selected.getUserId());
                    controller.displayWorkout(selected.getUserId());
                }
                else
                {
                    workout.setText("Select a mutual match to see your workout.");
                }
                updateComposer();
            }
        });
        send.addActionListener(event -> sendMessage());
        message.addActionListener(event -> sendMessage());
        addExercise.addActionListener(event -> addExercise());
        controller.displayNextProfile();
        controller.displayMatches();
        updateComposer();
        showInfo("Like each other to start a conversation.");
        this.updates.addPropertyChangeListener(updateListener);
        chat.addMessageListener(messageListener);
    }

    private static void styleMatches(JList<UserProfile> list)
    {
        list.setBackground(BACKGROUND);
        list.setForeground(INK);
        list.setSelectionBackground(new Color(30, 75, 46));
        list.setSelectionForeground(ACCENT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel)super.getListCellRendererComponent(
                    list, value, index, selected, focus);
                UserProfile profile = (UserProfile)value;
                label.putClientProperty("html.disable", true);
                label.setText(profile.getName() + "  /  " + profile.getUserId());
                label.setBorder(new EmptyBorder(9, 10, 9, 10));
                return label;
            }
        });
    }

    private static JPanel matchSidebar(JList<UserProfile> list)
    {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(180, 0));
        JLabel title = new JLabel("Your matches");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(INK);
        title.setLabelFor(list);
        sidebar.add(title, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(list), BorderLayout.CENTER);
        return sidebar;
    }

    private static JPanel matchWorkspace(JComponent content)
    {
        JPanel workspace = new JPanel(new BorderLayout(20, 0));
        workspace.setBackground(SURFACE);
        workspace.setBorder(new EmptyBorder(20, 20, 20, 20));
        workspace.add(content, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel workoutPanel()
    {
        workout.setName("workout");
        workout.setBackground(BACKGROUND);
        workout.setForeground(INK);
        workout.setEditable(false);
        workout.setLineWrap(true);
        workout.setWrapStyleWord(true);
        workout.setFont(new Font("SansSerif", Font.PLAIN, 17));
        workout.setMargin(new Insets(16, 16, 16, 16));
        workout.setText("Select a mutual match to see your workout.");

        JPanel counts = new JPanel(new GridLayout(1, 3, 12, 0));
        counts.setOpaque(false);
        counts.add(exerciseField("Sets", "exerciseSets", exerciseSets));
        counts.add(exerciseField("Reps", "exerciseReps", exerciseReps));
        counts.add(exerciseField("Weight (lb)", "exerciseWeight", exerciseWeight));
        addExercise.setPreferredSize(new Dimension(156, 44));
        JPanel action = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        action.setOpaque(false);
        action.add(addExercise);

        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 8));
        fields.setOpaque(false);
        fields.add(exerciseField("Exercise", "exerciseName", exerciseName));
        fields.add(counts);
        fields.add(action);
        JPanel routine = new JPanel(new BorderLayout(0, 16));
        routine.setOpaque(false);
        routine.add(new JScrollPane(workout), BorderLayout.CENTER);
        routine.add(fields, BorderLayout.SOUTH);
        return routine;
    }

    private static JPanel exerciseField(String text, String name, JTextField field)
    {
        JLabel label = new JLabel(text);
        label.setForeground(INK);
        label.setLabelFor(field);
        field.setName(name);
        field.setBackground(BACKGROUND);
        field.setForeground(INK);
        field.setCaretColor(ACCENT);
        field.setDisabledTextColor(MUTED);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(49, 75, 59)),
            new EmptyBorder(8, 10, 8, 10)));
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void addExercise()
    {
        UserProfile selected = matches.getSelectedValue();
        if (selected != null && controller.addWorkoutExercise(selected.getUserId(),
            exerciseName.getText(), exerciseSets.getText(), exerciseReps.getText(),
            exerciseWeight.getText()))
        {
            clearExerciseFields();
            updates.firePropertyChange("workout", controller.getCurrentUserId(),
                selected.getUserId());
        }
    }

    private void clearExerciseFields()
    {
        for (JTextField field : List.of(exerciseName, exerciseSets, exerciseReps, exerciseWeight))
        {
            field.setText("");
        }
    }

    private void showCreateProfile()
    {
        JTextField[] fields = new JTextField[6];
        String[] labels = { "Name", "Age", "Goal (optional)", "Gym (optional)",
            "Preferred time (optional)", "About me (optional)" };
        JComboBox<String> gym = new JComboBox<>(new String[] {
            "", "War Memorial Hall", "McComas Hall", "Venture Out Center",
            "Rec Sports Field House", "Washington Street Courts",
            "Prairie Quad Fitness Park", "VT Intramural Fields",
            "Sand Volleyball Courts", "VT Golf Course", "Disc Golf Course",
            "Drillfield", "Roller Hockey Rink", "Venture Out Challenge Course",
            "Blacksburg Health & Fitness at The Weight Club", "Anytime Fitness",
            "Zeffer Fitness", "460 Fitness", "Blacksburg Boxing and Fitness",
            "In Balance Yoga", "Blacksburg Community Center",
            "Crimper's Climbing Gym", "Blacksburg Aquatic Center",
            "Christiansburg Aquatic Center" });
        gym.setEditable(true);
        gym.setPreferredSize(new Dimension(260, 28));
        gym.setToolTipText("Facilities from the supplied VT/Blacksburg list; or type your own.");
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < 6; i++)
        {
            fields[i] = new JTextField();
            form.add(new JLabel(labels[i]));
            form.add(i == 3 ? gym : fields[i]);
        }
        JComboBox<String> activity = new JComboBox<>(new String[] {
            "Running", "Swimming", "Strength training", "Cycling", "Basketball",
            "Volleyball", "Badminton", "Pickleball", "Tennis", "Table tennis",
            "Racquetball", "Squash", "Boxing", "Yoga", "Pilates", "Dance",
            "CrossFit", "Bouldering", "Climbing", "Soccer", "Flag football",
            "Ultimate frisbee", "Golf", "Disc golf", "Roller hockey",
            "Calisthenics" });
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
                int userId = 1;
                while (sessions.containsKey(userId))
                {
                    userId++;
                }
                UserProfile user = new UserProfile(userId,
                    fields[0].getText(), Integer.parseInt(fields[1].getText().trim()),
                    java.util.Map.of((String)activity.getSelectedItem(),
                        (String)level.getSelectedItem()), fields[2].getText(),
                    gym.getEditor().getItem().toString(), fields[4].getText(), fields[5].getText());
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
        updates.firePropertyChange("profile", null, user);
        users.setSelectedItem(accountLabel(user));
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
        updates.firePropertyChange("swipe", null, null);
    }

    private void updateComposer()
    {
        boolean selected = matches.getSelectedValue() != null;
        message.setEnabled(selected);
        send.setEnabled(selected);
        addExercise.setEnabled(selected);
        for (JTextField field : List.of(exerciseName, exerciseSets, exerciseReps, exerciseWeight))
        {
            field.setEnabled(selected);
        }
        message.setToolTipText(selected ? "Write a message" : "Select a mutual match first");
    }

    private void sendMessage()
    {
        UserProfile selected = matches.getSelectedValue();
        if (selected != null && controller.sendChatMessage(
            selected.getUserId(), message.getText()))
        {
            message.setText("");
        }
    }

    private void refreshConversation(PropertyChangeEvent event)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> refreshConversation(event));
            return;
        }
        UserProfile selected = matches.getSelectedValue();
        int userId = controller.getCurrentUserId();
        int sender = (Integer)event.getOldValue();
        int recipient = (Integer)event.getNewValue();
        if (!closed && selected != null
            && ((userId == sender && selected.getUserId() == recipient)
                || (userId == recipient && selected.getUserId() == sender)))
        {
            controller.displayConversation(selected.getUserId());
        }
    }

    private void refreshPeer(PropertyChangeEvent event)
    {
        if ("workout".equals(event.getPropertyName()))
        {
            UserProfile selected = matches.getSelectedValue();
            int userId = controller.getCurrentUserId();
            int author = (Integer)event.getOldValue();
            int partner = (Integer)event.getNewValue();
            if (selected != null && ((userId == author && selected.getUserId() == partner)
                || (userId == partner && selected.getUserId() == author)))
            {
                controller.displayWorkout(selected.getUserId());
            }
            return;
        }
        if ("profile".equals(event.getPropertyName()))
        {
            addAccount((UserProfile)event.getNewValue());
        }
        controller.displayNextProfile();
        controller.displayMatches();
    }

    private void addAccount(UserProfile user)
    {
        users.addItem(accountLabel(user));
    }

    private static String accountLabel(UserProfile user)
    {
        return user.getName() + " / " + user.getUserId();
    }

    @Override
    public void showProfile(UserProfile profile)
    {
        boolean available = profile != null;
        pass.setEnabled(available);
        like.setEnabled(available);
        avatar.setIcon(null);
        if (available)
        {
            String file = java.util.Map.of(1, "demo-1.jpg", 2, "demo-2.jpg",
                3, "demo-7.jpg", 4, "demo-3.jpg").get(profile.getUserId());
            java.net.URL resource = file == null ? null : getClass().getResource("photos/" + file);
            if (resource != null)
            {
                ImageIcon original = new ImageIcon(resource);
                if (original.getIconWidth() > 0 && original.getIconHeight() > 0)
                {
                    int height = 290;
                    int width = original.getIconWidth() * height / original.getIconHeight();
                    avatar.setIcon(new ImageIcon(original.getImage().getScaledInstance(
                        Math.max(1, width), height, Image.SCALE_SMOOTH)));
                }
            }
        }
        avatar.setText(avatar.getIcon() != null ? "" : available ? profile.getName().substring(0, 1).toUpperCase() : "-");
        name.setText(available ? profile.getName() + ", " + profile.getAge() : "All caught up");
        details.setText(available ? "Interests: " + profile.getWorkoutExperience()
            + "\nGoal: " + profile.getGoal() + "\nGym: " + profile.getGym()
            + "\nTime: " + profile.getPreferredTime() + "\nAbout: " + profile.getBio()
            : "No eligible profiles left. Try another demo account.");
    }

    @Override
    public void showMatches(List<UserProfile> profiles)
    {
        UserProfile selected = matches.getSelectedValue();
        if (profiles.equals(java.util.Collections.list(matchItems.elements())))
        {
            if (selected == null)
            {
                showChatPrompt(profiles);
            }
            return;
        }
        matchItems.clear();
        for (UserProfile profile : profiles)
        {
            matchItems.addElement(profile);
        }
        if (selected != null && profiles.contains(selected))
        {
            matches.setSelectedValue(selected, true);
            return;
        }
        showChatPrompt(profiles);
    }

    private void showChatPrompt(List<UserProfile> profiles)
    {
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
    public void showWorkout(List<Workout.Exercise> exercises)
    {
        workout.setText(formatWorkout(exercises));
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

    SpotterSwingView openPeerView(int userId)
    {
        return new SpotterSwingView(sessions, userId, updates);
    }

    void close()
    {
        closed = true;
        updates.removePropertyChangeListener(updateListener);
        chat.removeMessageListener(messageListener);
    }

    private void showWindow(int offset)
    {
        JFrame window = new JFrame("Spotter - Gym partners");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent event)
            {
                close();
            }
        });
        window.setContentPane(panel);
        window.pack();
        window.setMinimumSize(new Dimension(900, 800));
        window.setLocationRelativeTo(null);
        window.setLocation(window.getX() + offset, window.getY() + offset);
        window.setVisible(true);
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            SpotterSwingView view = new SpotterSwingView();
            view.showWindow(-35);
            view.openPeerView(2).showWindow(35);
        });
    }
}

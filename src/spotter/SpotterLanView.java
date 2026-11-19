package spotter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.*;
import static spotter.SpotterLanStyle.*;

/** Signed-in discovery and chat for the shared LAN app. */
public final class SpotterLanView
{
    private static final int HISTORY_LIMIT = 100;
    private static final int PROFILE_PHOTO_EDGE = 360;

    private record Action(String name, int peer, String text, String requestId)
    {
        boolean isPoll()
        {
            return "view".equals(name);
        }

        boolean isSend()
        {
            return "send".equals(name);
        }
    }

    private record Candidate(int id, SpotterLanClient.Profile profile, ImageIcon photo)
    {
    }

    private record Discovery(SpotterLanClient.Snapshot snapshot, Map<Integer, String> names,
        Candidate candidate)
    {
    }

    static final class Drafts
    {
        private record Draft(String text, String requestId)
        {
        }

        private final Map<Integer, Draft> byPeer = new HashMap<>();

        String text(int peer)
        {
            Draft draft = byPeer.get(peer);
            return draft == null ? "" : draft.text();
        }

        void save(int peer, String text)
        {
            if (peer != 0 && !text.equals(text(peer)))
            {
                if (text.isEmpty())
                {
                    byPeer.remove(peer);
                }
                else
                {
                    byPeer.put(peer, new Draft(text, ""));
                }
            }
        }

        String idForSend(int peer, String text)
        {
            save(peer, text);
            Draft draft = byPeer.get(peer);
            if (draft.requestId().isEmpty())
            {
                draft = new Draft(text, UUID.randomUUID().toString());
                byPeer.put(peer, draft);
            }
            return draft.requestId();
        }

        boolean complete(int peer, String requestId)
        {
            Draft draft = byPeer.get(peer);
            return draft != null && draft.requestId().equals(requestId)
                && byPeer.remove(peer, draft);
        }
    }

    private final SpotterLanClient client;
    private final URI server;
    private final Map<Integer, String> names = new HashMap<>();
    private final JFrame window = new JFrame("Spotter");
    private final JLabel candidate = heading("Connecting…");
    private final JLabel candidatePhoto = new JLabel("", SwingConstants.CENTER);
    private final JTextArea candidateDetails = new JTextArea(3, 34);
    private final JButton pass = new JButton("Pass");
    private final JButton like = new JButton("Like");
    private final JButton logout = new JButton("Log out");
    private final JComboBox<Integer> matches = new JComboBox<>();
    private final JTextArea history = new JTextArea(16, 40);
    private final JTextField message = new JTextField();
    private final JButton send = new JButton("Send");
    private final JLabel status = new JLabel("Connecting…");
    private final Timer refresh = new Timer(1000, event -> request("view", selectedPeer()));
    private final Drafts drafts = new Drafts();
    private final Deque<String> displayedHistory = new ArrayDeque<>();
    private List<Integer> displayedMatches = List.of();
    private Candidate displayedCandidate;
    private int candidateId = -1;
    private int openPeer;
    private int cursor;
    private Action active;
    private Action queued;
    private boolean signingOut;

    private SpotterLanView(URI server, SpotterLanClient client)
    {
        this.server = server;
        this.client = client;
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading("Spotter"), BorderLayout.WEST);
        button(logout, false);
        header.add(logout, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        JTabbedPane sections = new JTabbedPane();
        sections.addTab("Discover", discoveryPanel());
        sections.addTab("Messages", conversationPanel());
        content.add(sections, BorderLayout.CENTER);
        status.putClientProperty("html.disable", true);
        status.setForeground(MUTED);
        content.add(status, BorderLayout.SOUTH);
        window.setContentPane(content);

        logout.addActionListener(event -> signOut());
        pass.addActionListener(event -> request("left", candidateId));
        like.addActionListener(event -> request("right", candidateId));
        send.addActionListener(event -> request("send", selectedPeer()));
        message.addActionListener(event -> request("send", selectedPeer()));
        matches.addActionListener(event -> changePeer());
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event)
            {
                refresh.stop();
            }
        });
        window.pack();
        window.setLocationByPlatform(true);
        window.setVisible(true);
        request("view", 0);
        refresh.start();
    }

    private JPanel conversationPanel()
    {
        JLabel matchLabel = new JLabel("Mutual match");
        matchLabel.setForeground(INK);
        matches.setToolTipText("Choose a mutual match to chat");
        matchLabel.setLabelFor(matches);
        JPanel matchPicker = new JPanel();
        matchPicker.setOpaque(false);
        matchPicker.add(matchLabel);
        matchPicker.add(matches);
        JPanel chatHeader = new JPanel(new BorderLayout(8, 0));
        chatHeader.setOpaque(false);
        chatHeader.add(heading("Your matches"), BorderLayout.WEST);
        chatHeader.add(matchPicker, BorderLayout.CENTER);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.putClientProperty("html.disable", true);
        matches.setRenderer((list, id, index, selected, focus) ->
            renderer.getListCellRendererComponent(list, name(id), index, selected, focus));
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        input(history);
        input(message);
        button(send, true);
        history.getAccessibleContext().setAccessibleName("Conversation");
        message.getAccessibleContext().setAccessibleName("Message");
        JPanel composer = new JPanel(new BorderLayout(8, 8));
        composer.setOpaque(false);
        composer.add(message, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);

        JPanel chat = new JPanel(new BorderLayout(0, 12));
        chat.setBackground(SURFACE);
        chat.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        chat.add(chatHeader, BorderLayout.NORTH);
        JScrollPane transcript = new JScrollPane(history);
        transcript.setBorder(BorderFactory.createEmptyBorder());
        chat.add(transcript, BorderLayout.CENTER);
        chat.add(composer, BorderLayout.SOUTH);
        return chat;
    }

    private JPanel discoveryPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        candidate.setForeground(INK);
        candidate.putClientProperty("html.disable", true);
        candidatePhoto.setPreferredSize(new java.awt.Dimension(400, PROFILE_PHOTO_EDGE));
        candidatePhoto.setBackground(BACKGROUND);
        candidatePhoto.setForeground(MUTED);
        candidatePhoto.setOpaque(true);
        candidatePhoto.getAccessibleContext().setAccessibleName("Candidate profile photo");
        candidateDetails.setEditable(false);
        candidateDetails.setLineWrap(true);
        candidateDetails.setWrapStyleWord(true);
        candidateDetails.setBackground(SURFACE);
        candidateDetails.setForeground(INK);
        candidateDetails.setBorder(BorderFactory.createEmptyBorder());
        candidateDetails.getAccessibleContext().setAccessibleName("Candidate profile details");
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        button(pass, false);
        button(like, true);
        buttons.add(pass);
        buttons.add(like);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(candidate, BorderLayout.NORTH);
        JScrollPane details = new JScrollPane(candidateDetails);
        details.setBorder(BorderFactory.createEmptyBorder());
        footer.add(details, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        panel.add(candidatePhoto, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private String name(Integer id)
    {
        return id == null ? "" : names.getOrDefault(id, "Profile #" + id);
    }

    private int selectedPeer()
    {
        Integer selected = (Integer)matches.getSelectedItem();
        return selected == null ? 0 : selected;
    }

    private void updateActions()
    {
        boolean ready = acceptsAction();
        pass.setEnabled(ready && displayedCandidate != null);
        like.setEnabled(ready && displayedCandidate != null);
        send.setEnabled(ready && openPeer != 0);
        message.setEnabled(!signingOut && openPeer != 0);
        logout.setEnabled(ready);
    }

    private void changePeer()
    {
        int selected = selectedPeer();
        if (selected != openPeer)
        {
            drafts.save(openPeer, message.getText());
            openPeer = selected;
            message.setText(drafts.text(openPeer));
            displayedHistory.clear();
            cursor = 0;
            history.setText("");
        }
        updateActions();
    }

    private boolean acceptsAction()
    {
        return !signingOut && queued == null && (active == null || active.isPoll());
    }

    private void request(String name, int peer)
    {
        boolean poll = "view".equals(name);
        if (signingOut || !window.isDisplayable() || (poll ? active != null : !acceptsAction()))
        {
            return;
        }
        boolean sending = "send".equals(name);
        String text = sending ? message.getText() : "";
        if (sending && (peer == 0 || text.isBlank() || text.length() > 500))
        {
            alert(window, "Check your message", "Your message couldn’t be sent.",
                "Select a mutual match and enter a message between 1 and 500 characters.");
            return;
        }
        Action action = new Action(name, peer, text, sending ? drafts.idForSend(peer, text) : "");
        if (active == null)
        {
            perform(action);
        }
        else
        {
            queued = action;
            updateActions();
        }
    }

    private void perform(Action action)
    {
        active = action;
        updateActions();
        int after = action.peer() == openPeer ? cursor : 0;
        Candidate previousCandidate = displayedCandidate;
        Map<Integer, String> previousNames = Map.copyOf(names);
        new SwingWorker<Discovery, Void>()
        {
            @Override
            protected Discovery doInBackground() throws Exception
            {
                return fetchDiscovery(action, after, previousCandidate, previousNames);
            }

            @Override
            protected void done()
            {
                if (signingOut || !window.isDisplayable())
                {
                    return;
                }
                try
                {
                    Discovery discovery = get();
                    showSnapshot(discovery, action.peer(), after);
                    reportResult(action, discovery.snapshot().accepted());
                }
                catch (ExecutionException error)
                {
                    if (error.getCause() instanceof SpotterLanClient.SessionExpiredException)
                    {
                        alert(window, "Session expired", "Your session has ended.",
                            "Log in again to continue.");
                        returnToLogin("Your session expired. Log in again.");
                        return;
                    }
                    if (action.isPoll())
                    {
                        status.setText("Connection error. Check that the server is running.");
                    }
                    else if (action.isSend())
                    {
                        alert(window, "Delivery unconfirmed", "Spotter could not confirm delivery.",
                            "Check your connection and retry the same draft.");
                    }
                    else
                    {
                        alert(window, "Connection error", "Spotter couldn’t complete that action.",
                            "Check that the server is running, then try again.");
                    }
                }
                catch (InterruptedException error)
                {
                    Thread.currentThread().interrupt();
                    status.setText("Request interrupted.");
                    if (!action.isPoll())
                    {
                        alert(window, "Request interrupted", "The action was interrupted.",
                            "Check your connection and try again.");
                    }
                }
                active = null;
                Action next = queued;
                queued = null;
                if (next == null)
                {
                    updateActions();
                }
                else
                {
                    perform(next);
                }
            }
        }.execute();
    }

    private Discovery fetchDiscovery(Action action, int after, Candidate previousCandidate,
        Map<Integer, String> previousNames) throws IOException, InterruptedException
    {
        SpotterLanClient.Snapshot snapshot = client.request(action.name(), action.peer(),
            action.text(), action.requestId(), after);
        Candidate card = loadCandidate(snapshot.candidate(), previousCandidate);
        Map<Integer, String> updatedNames = new HashMap<>();
        if (card != null)
        {
            updatedNames.put(card.id(), card.profile().name());
        }
        for (int match : snapshot.matches())
        {
            if (!updatedNames.containsKey(match))
            {
                String existing = previousNames.get(match);
                SpotterLanClient.Profile profile = existing == null ? findProfile(match) : null;
                if (profile != null || existing != null)
                {
                    updatedNames.put(match, existing == null ? profile.name() : existing);
                }
            }
        }
        return new Discovery(snapshot, updatedNames, card);
    }

    private Candidate loadCandidate(int id, Candidate previous)
        throws IOException, InterruptedException
    {
        if (id == 0 || (previous != null && previous.id() == id))
        {
            return id == 0 ? null : previous;
        }
        SpotterLanClient.Profile profile = findProfile(id);
        if (profile == null)
        {
            return null;
        }
        ImageIcon image = null;
        try
        {
            var decoded = ImageIO.read(new ByteArrayInputStream(profile.photo()));
            if (decoded != null)
            {
                image = SpotterLanOnboarding.preview(decoded, PROFILE_PHOTO_EDGE);
            }
        }
        catch (IOException invalidImage)
        {
            // A missing thumbnail must not prevent discovery or hide a successful send.
        }
        return new Candidate(id, profile, image);
    }

    private SpotterLanClient.Profile findProfile(int id) throws IOException, InterruptedException
    {
        try
        {
            return client.profile(id);
        }
        catch (SpotterLanClient.SessionExpiredException expired)
        {
            throw expired;
        }
        catch (IOException unavailable)
        {
            return null;
        }
    }

    private void reportResult(Action action, boolean accepted)
    {
        if (!accepted)
        {
            if (!action.isPoll())
            {
                alert(window, "Action couldn’t be completed", "The server rejected that action.",
                    action.isSend() ? "Choose a current mutual match and check your message."
                        : "Wait for the profile to refresh before choosing Like or Pass.");
            }
        }
        else if (!action.isPoll())
        {
            status.setText(action.isSend() ? "Message sent to " + name(action.peer()) + "."
                : "Action completed.");
            if (action.isSend())
            {
                drafts.save(openPeer, message.getText());
                if (drafts.complete(action.peer(), action.requestId()) && action.peer() == openPeer)
                {
                    message.setText("");
                }
            }
        }
        else if (status.getText().equals("Connecting…")
            || status.getText().startsWith("Connection error"))
        {
            status.setText("Connected.");
        }
    }

    private void showSnapshot(Discovery discovery, int peer, int after)
    {
        SpotterLanClient.Snapshot snapshot = discovery.snapshot();
        boolean candidateChanged = candidateId != snapshot.candidate()
            || displayedCandidate != discovery.candidate();
        candidateId = snapshot.candidate();
        names.clear();
        names.putAll(discovery.names());
        if (candidateChanged)
        {
            showCandidate(discovery.candidate());
        }
        if (!displayedMatches.equals(snapshot.matches()))
        {
            int selected = selectedPeer();
            displayedMatches = snapshot.matches();
            DefaultComboBoxModel<Integer> model =
                new DefaultComboBoxModel<>(displayedMatches.toArray(Integer[]::new));
            if (displayedMatches.contains(selected))
            {
                model.setSelectedItem(selected);
            }
            matches.setModel(model);
            changePeer();
        }
        matches.repaint();
        if (peer == openPeer && after == cursor)
        {
            appendHistory(snapshot, after);
        }
    }

    private void showCandidate(Candidate card)
    {
        displayedCandidate = card;
        candidatePhoto.setIcon(card == null ? null : card.photo());
        candidatePhoto.setText(card != null && card.photo() != null ? "" : candidateId == 0
            ? "New training partners will appear here" : "Photo unavailable");
        if (card == null)
        {
            candidate.setText(candidateId == 0 ? "No eligible profiles yet" : "Profile temporarily unavailable");
            candidateDetails.setText("");
            return;
        }
        SpotterLanClient.Profile profile = card.profile();
        candidate.setText(profile.name() + ", " + profile.age());
        String details = "Workout interest: " + profile.interest();
        if (!profile.bio().isBlank())
        {
            details += "\n\n" + profile.bio();
        }
        candidateDetails.setText(details);
        candidateDetails.setCaretPosition(0);
    }

    private void signOut()
    {
        signingOut = true;
        refresh.stop();
        updateActions();
        status.setText("Logging out…");
        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                client.logout();
                return null;
            }

            @Override
            protected void done()
            {
                if (!window.isDisplayable())
                {
                    return;
                }
                String notice = "You are logged out.";
                try
                {
                    get();
                }
                catch (ExecutionException error)
                {
                    notice = "Signed out on this computer. Server logout was unconfirmed.";
                    alert(window, "Server logout unconfirmed", notice,
                        "Check your connection before logging back in.");
                }
                catch (InterruptedException error)
                {
                    Thread.currentThread().interrupt();
                    notice = "Signed out on this computer. Server logout was interrupted.";
                }
                returnToLogin(notice);
            }
        }.execute();
    }

    private void returnToLogin(String notice)
    {
        refresh.stop();
        window.dispose();
        new SpotterLanOnboarding(server, notice, SpotterLanView::new);
    }

    private void appendHistory(SpotterLanClient.Snapshot snapshot, int after)
    {
        if (snapshot.start() != after)
        {
            displayedHistory.clear();
        }
        if (!snapshot.history().isEmpty())
        {
            displayedHistory.addAll(List.of(snapshot.history().split("\n")));
            while (displayedHistory.size() > HISTORY_LIMIT)
            {
                displayedHistory.removeFirst();
            }
        }
        cursor = snapshot.next();
        String display = openPeer == 0 ? "Match with a training partner to start a conversation."
            : String.join("\n", displayedHistory);
        if (!history.getText().equals(display))
        {
            history.setText(display);
            history.setCaretPosition(history.getDocument().getLength());
        }
    }

    public static void main(String[] args)
    {
        URI server = args.length == 0 ? null : URI.create(args[0]);
        SwingUtilities.invokeLater(() -> new SpotterLanOnboarding(server,
            "Log in or create an account.", SpotterLanView::new));
    }
}

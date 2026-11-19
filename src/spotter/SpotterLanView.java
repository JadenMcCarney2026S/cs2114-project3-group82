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

/** Signed-in discovery and chat for the shared LAN app. */
public final class SpotterLanView
{
    private static final int HISTORY_LIMIT = 100;

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
    private final JLabel candidate = new JLabel("Connecting…");
    private final JLabel candidatePhoto = new JLabel("", SwingConstants.CENTER);
    private final JTextArea candidateDetails = new JTextArea(5, 24);
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
        JPanel chat = conversationPanel();
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(discoveryPanel(), BorderLayout.WEST);
        content.add(chat, BorderLayout.CENTER);
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
        matchLabel.setLabelFor(matches);
        JPanel matchPicker = new JPanel();
        matchPicker.add(matchLabel);
        matchPicker.add(matches);
        matchPicker.add(logout);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.putClientProperty("html.disable", true);
        matches.setRenderer((list, id, index, selected, focus) ->
            renderer.getListCellRendererComponent(list, name(id), index, selected, focus));
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        history.getAccessibleContext().setAccessibleName("Conversation");
        message.getAccessibleContext().setAccessibleName("Message");
        JPanel composer = new JPanel(new BorderLayout());
        composer.add(message, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);
        composer.add(status, BorderLayout.SOUTH);
        status.putClientProperty("html.disable", true);

        JPanel chat = new JPanel(new BorderLayout(0, 8));
        chat.add(matchPicker, BorderLayout.NORTH);
        chat.add(new JScrollPane(history), BorderLayout.CENTER);
        chat.add(composer, BorderLayout.SOUTH);
        return chat;
    }

    private JPanel discoveryPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel title = new JPanel(new GridLayout(0, 1));
        title.add(new JLabel("Discover"));
        candidate.putClientProperty("html.disable", true);
        title.add(candidate);
        candidatePhoto.setPreferredSize(new java.awt.Dimension(240, 240));
        candidatePhoto.getAccessibleContext().setAccessibleName("Candidate profile photo");
        candidateDetails.setEditable(false);
        candidateDetails.setLineWrap(true);
        candidateDetails.setWrapStyleWord(true);
        candidateDetails.getAccessibleContext().setAccessibleName("Candidate profile details");
        JPanel buttons = new JPanel();
        buttons.add(pass);
        buttons.add(like);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(new JScrollPane(candidateDetails), BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        panel.add(title, BorderLayout.NORTH);
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
            status.setText("Select a match and enter 1–500 characters.");
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
                        returnToLogin("Your session expired. Log in again.");
                        return;
                    }
                    status.setText(action.isSend()
                        ? "Delivery to " + name(action.peer()) + " unconfirmed. Retry the same draft."
                        : "Connection error. " + error.getCause().getMessage());
                }
                catch (InterruptedException error)
                {
                    Thread.currentThread().interrupt();
                    status.setText("Request interrupted.");
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
                image = SpotterLanOnboarding.preview(decoded, 240);
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
            status.setText("Action rejected. Check the current match or profile.");
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
        candidatePhoto.setText(card != null && card.photo() == null ? "Photo unavailable" : "");
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
        String display = String.join("\n", displayedHistory);
        if (!history.getText().equals(display))
        {
            history.setText(display);
            history.setCaretPosition(history.getDocument().getLength());
        }
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new SpotterLanOnboarding(null,
            "Log in or create an account.", SpotterLanView::new));
    }
}

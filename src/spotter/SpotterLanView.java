package spotter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/** Desktop client for the shared LAN demo. */
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
    private final Map<Integer, MainLogic> profiles = SpotterApp.createSessions();
    private final JFrame window = new JFrame("Spotter LAN");
    private final JLabel candidate = new JLabel("Connecting…");
    private final JButton pass = new JButton("Pass");
    private final JButton like = new JButton("Like");
    private final JComboBox<Integer> matches = new JComboBox<>();
    private final JTextArea history = new JTextArea(16, 40);
    private final JTextField message = new JTextField();
    private final JButton send = new JButton("Send");
    private final JLabel status = new JLabel("Connecting…");
    private final Timer refresh = new Timer(1000, event -> request("view", selectedPeer()));
    private final Drafts drafts = new Drafts();
    private final Deque<String> displayedHistory = new ArrayDeque<>();
    private List<Integer> displayedMatches = List.of();
    private int candidateId;
    private int openPeer;
    private int cursor;
    private Action active;
    private Action queued;

    private SpotterLanView(SpotterLanClient client)
    {
        this.client = client;
        JPanel discover = new JPanel();
        discover.add(candidate);
        discover.add(pass);
        discover.add(like);

        JLabel matchLabel = new JLabel("Mutual match");
        matchLabel.setLabelFor(matches);
        JPanel matchPicker = new JPanel();
        matchPicker.add(matchLabel);
        matchPicker.add(matches);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        matches.setRenderer((list, id, index, selected, focus) ->
            renderer.getListCellRendererComponent(list, name(id), index, selected, focus));

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.add(discover);
        header.add(matchPicker);
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        history.getAccessibleContext().setAccessibleName("Conversation");
        message.getAccessibleContext().setAccessibleName("Message");
        JPanel composer = new JPanel(new BorderLayout());
        composer.add(message, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);
        composer.add(status, BorderLayout.SOUTH);

        window.add(header, BorderLayout.NORTH);
        window.add(new JScrollPane(history), BorderLayout.CENTER);
        window.add(composer, BorderLayout.SOUTH);
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

    private String name(Integer id)
    {
        MainLogic profile = profiles.get(id);
        return profile == null ? "" : profile.getCurrentUser().getName();
    }

    private int selectedPeer()
    {
        Integer selected = (Integer)matches.getSelectedItem();
        return selected == null ? 0 : selected;
    }

    private void updateActions()
    {
        boolean ready = acceptsAction();
        pass.setEnabled(ready && candidateId != 0);
        like.setEnabled(ready && candidateId != 0);
        send.setEnabled(ready && openPeer != 0);
        message.setEnabled(openPeer != 0);
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
        return queued == null && (active == null || active.isPoll());
    }

    private void request(String name, int peer)
    {
        boolean poll = "view".equals(name);
        if (!window.isDisplayable() || (poll ? active != null : !acceptsAction()))
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
        new SwingWorker<SpotterLanClient.Snapshot, Void>()
        {
            @Override
            protected SpotterLanClient.Snapshot doInBackground() throws Exception
            {
                return client.request(action.name(), action.peer(), action.text(),
                    action.requestId(), after);
            }

            @Override
            protected void done()
            {
                if (!window.isDisplayable())
                {
                    return;
                }
                try
                {
                    SpotterLanClient.Snapshot snapshot = get();
                    showSnapshot(snapshot, action.peer(), after);
                    reportResult(action, snapshot.accepted());
                }
                catch (ExecutionException error)
                {
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

    private void showSnapshot(SpotterLanClient.Snapshot snapshot, int peer, int after)
    {
        candidateId = snapshot.candidate();
        candidate.setText(candidateId == 0 ? "No eligible profiles" : name(candidateId));
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
        if (peer == openPeer && after == cursor)
        {
            appendHistory(snapshot, after);
        }
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
        SwingUtilities.invokeLater(() -> {
            JTextField address = new JTextField("https://192.168.1.10:8080/chat", 25);
            JPasswordField code = new JPasswordField(25);
            JPanel form = new JPanel(new GridLayout(0, 1));
            JLabel addressLabel = new JLabel("Server URL");
            addressLabel.setLabelFor(address);
            JLabel codeLabel = new JLabel("Demo code");
            codeLabel.setLabelFor(code);
            form.add(addressLabel);
            form.add(address);
            form.add(codeLabel);
            form.add(code);
            while (JOptionPane.showConfirmDialog(null, form, "Connect to Spotter LAN",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            {
                try
                {
                    new SpotterLanView(new SpotterLanClient(URI.create(address.getText().trim()),
                        new String(code.getPassword()).trim()));
                    return;
                }
                catch (IllegalArgumentException error)
                {
                    JOptionPane.showMessageDialog(null, "Enter a valid server URL and demo code.");
                }
            }
        });
    }
}

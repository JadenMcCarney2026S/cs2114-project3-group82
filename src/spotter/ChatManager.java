package spotter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared conversations; authorization uses the same state as swipe processing. */
public class ChatManager
{
    private final MatchManager matchManager;
    private final Map<String, List<Message>> conversations = new HashMap<>();
    private final PropertyChangeSupport updates = new PropertyChangeSupport(this);

    public ChatManager(MatchManager matchManager)
    {
        if (matchManager == null)
        {
            throw new IllegalArgumentException("Match manager cannot be null");
        }
        this.matchManager = matchManager;
    }

    MatchManager getMatchManager()
    {
        return matchManager;
    }

    void addMessageListener(PropertyChangeListener listener)
    {
        updates.addPropertyChangeListener(listener);
    }

    void removeMessageListener(PropertyChangeListener listener)
    {
        updates.removePropertyChangeListener(listener);
    }

    private String conversationKey(int userId, int otherUserId)
    {
        return Math.min(userId, otherUserId) + ":" + Math.max(userId, otherUserId);
    }

    public boolean sendMessage(int senderId, int recipientId, String content)
    {
        if (!matchManager.areMatched(senderId, recipientId)
            || content == null || content.trim().isEmpty())
        {
            return false;
        }
        conversations.computeIfAbsent(conversationKey(senderId, recipientId),
            key -> new ArrayList<>()).add(new Message(senderId, content));
        updates.firePropertyChange("chat", senderId, recipientId);
        return true;
    }

    public List<String> getConversation(int userId, int otherUserId)
    {
        List<String> result = new ArrayList<>();
        if (!matchManager.areMatched(userId, otherUserId))
        {
            return result;
        }
        for (Message message : conversations.getOrDefault(
            conversationKey(userId, otherUserId), List.of()))
        {
            result.add(message.getSenderId() + ": " + message.getMessageText());
        }
        return result;
    }
}

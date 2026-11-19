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
    record History(int start, int next, List<String> messages)
    {
    }

    private static final class Conversation
    {
        final List<Message> messages = new ArrayList<>();
        int start;
    }

    private final MatchManager matchManager;
    private final Map<String, Conversation> conversations = new HashMap<>();
    private final int historyLimit;
    private final PropertyChangeSupport updates = new PropertyChangeSupport(this);

    public ChatManager(MatchManager matchManager)
    {
        this(matchManager, Integer.MAX_VALUE);
    }

    ChatManager(MatchManager matchManager, int historyLimit)
    {
        if (matchManager == null)
        {
            throw new IllegalArgumentException("Match manager cannot be null");
        }
        this.matchManager = matchManager;
        this.historyLimit = historyLimit;
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
        Conversation conversation = conversations.computeIfAbsent(
            conversationKey(senderId, recipientId), key -> new Conversation());
        conversation.messages.add(new Message(senderId, content));
        if (conversation.messages.size() > historyLimit)
        {
            conversation.messages.remove(0);
            conversation.start++;
        }
        updates.firePropertyChange("chat", senderId, recipientId);
        return true;
    }

    public List<String> getConversation(int userId, int otherUserId)
    {
        return getHistory(userId, otherUserId, 0).messages();
    }

    History getHistory(int userId, int otherUserId, int after)
    {
        if (!matchManager.areMatched(userId, otherUserId))
        {
            return new History(0, 0, new ArrayList<>());
        }
        Conversation conversation = conversations.get(conversationKey(userId, otherUserId));
        if (conversation == null)
        {
            return new History(0, 0, new ArrayList<>());
        }
        int next = conversation.start + conversation.messages.size();
        int start = after > next ? conversation.start : Math.max(after, conversation.start);
        List<String> result = new ArrayList<>();
        for (Message message : conversation.messages.subList(start - conversation.start,
            conversation.messages.size()))
        {
            result.add(message.getSenderId() + ": " + message.getMessageText());
        }
        return new History(start, next, result);
    }
}

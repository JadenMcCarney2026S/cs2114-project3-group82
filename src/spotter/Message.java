package spotter;

public class Message
{
    private final int senderId;
    private final String messageText;
    private final long timestamp;

    public Message(int senderId, String messageText)
    {
        if (messageText == null || messageText.trim().isEmpty())
        {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        this.senderId = senderId;
        this.messageText = messageText;
        timestamp = System.currentTimeMillis();
    }

    public int getSenderId()
    {
        return senderId;
    }

    public String getMessageText()
    {
        return messageText;
    }

    public long getTimestamp()
    {
        return timestamp;
    }
}

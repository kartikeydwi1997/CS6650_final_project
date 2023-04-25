import java.io.Serializable;

/**
 * Defines an object of the chat message sent by any client
 * in a chat room. Each message has a sender and room attached to it.
 * A message contains the content as well as the Lamport timestamp.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String room;
    private String content;
    private int timestamp;

    /**
     * Each message is sent by a client to a particular room.
     * @param sender client sending the message
     * @param room room the message is sent to
     * @param content the content of the message
     */
    public ChatMessage(String sender, String room, String content) {
        this.sender = sender;
        this.room = room;
        this.content = content;
    }

    /**
     * Sets the lamport timestamp of the message.
     * @param timestamp lamport timestamp
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the lamport timestamp of the message.
     * @return lamport timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the client id of the message.
     * @return client id who sent the message
     */
    public String getSender() {
        return sender;
    }

    /**
     * Gets the room the message was sent to.
     * @return room id where message was sent
     */
    public String getRoom() {
        return room;
    }

    /**
     * @return the message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the message entered by the client
     * @param content content of message
     */
    public void setContent(String content) {
        this.content = content;
    }
}

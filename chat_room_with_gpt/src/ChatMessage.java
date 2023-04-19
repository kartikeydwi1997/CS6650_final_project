import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sender;
    private String room;
    private String content;
    private int timestamp;

    public ChatMessage(String sender, String room, String content) {
        this.sender = sender;
        this.room = room;
        this.content = content;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getRoom() {
        return room;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

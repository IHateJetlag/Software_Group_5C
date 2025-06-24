package share;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String sender;    // 送信者のユーザー名
    private String groupId;   // 送信先のグループID
    private String message;
    private LocalDateTime timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String id, String sender, String groupId, String message, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.groupId = groupId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
               "id='" + id + '\'' +
               ", sender='" + sender + '\'' +
               ", groupId='" + groupId + '\'' +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
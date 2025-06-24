package share;

import java.io.Serializable;

public class ChatRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;
    private String message; // メッセージ本文

    public ChatRequest() {
    }

    public ChatRequest(String groupId, String message) {
        this.groupId = groupId;
        this.message = message;
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
}
import com.google.gson.JsonElement;
import java.util.List;

// サーバーからクライアントへのメッセージ
class ClientMessage {
    private String type;
    private JsonElement data;
    
    public ClientMessage() {}
    
    public ClientMessage(String type, JsonElement data) {
        this.type = type;
        this.data = data;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public JsonElement getData() { return data; }
    public void setData(JsonElement data) { this.data = data; }
}

// クライアントからサーバーへのメッセージ
class ServerMessage {
    private String type;
    private JsonElement data;
    
    public ServerMessage() {}
    
    public ServerMessage(String type, JsonElement data) {
        this.type = type;
        this.data = data;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public JsonElement getData() { return data; }
    public void setData(JsonElement data) { this.data = data; }
}

// ログインリクエスト
class LoginRequest {
    private String username;
    private String password;
    
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

// ログインレスポンス
class LoginResponse {
    private boolean success;
    private String message;
    
    public LoginResponse() {}
    
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

// 登録リクエスト
class RegisterRequest {
    private String username;
    private String password;
    private String email;
    
    public RegisterRequest() {}
    
    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// 登録レスポンス
class RegisterResponse {
    private boolean success;
    private String message;
    
    public RegisterResponse() {}
    
    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

// チャットリクエスト
class ChatRequest {
    private String groupId;
    private String message;
    
    public ChatRequest() {}
    
    public ChatRequest(String groupId, String message) {
        this.groupId = groupId;
        this.message = message;
    }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

// グループ作成リクエスト
class CreateGroupRequest {
    private String groupName;
    private List<String> members;
    
    public CreateGroupRequest() {}
    
    public CreateGroupRequest(String groupName, List<String> members) {
        this.groupName = groupName;
        this.members = members;
    }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
}

// エラーレスポンス
class ErrorResponse {
    private String message;
    
    public ErrorResponse() {}
    
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
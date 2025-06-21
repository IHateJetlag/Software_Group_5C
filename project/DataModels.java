import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ユーザークラス
class User {
    private String username;
    private String password;
    private String email;
    private LocalDateTime createdAt;
    
    public User() {}
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

// グループクラス
class Group {
    private String id;
    private String name;
    private String createdBy;
    private List<String> members;
    private LocalDateTime createdAt;
    
    public Group() {
        this.members = new ArrayList<>();
    }
    
    public Group(String id, String name, String createdBy, List<String> members, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

// スケジュールクラス
class Schedule {
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String groupId;
    private List<String> participants;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean isPrivate;
    
    public Schedule() {
        this.participants = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPrivate() { return isPrivate; } // <<<--- 追加
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; } // <<<--- 追加
}

// チャットメッセージクラス
class ChatMessage {
    private String id;
    private String sender;
    private String groupId;
    private String message;
    private LocalDateTime timestamp;
    
    public ChatMessage() {}
    
    public ChatMessage(String id, String sender, String groupId, String message, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.groupId = groupId;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// ユーザーデータ集約クラス
class UserData {
    private User user;
    private List<Group> groups;
    private List<Schedule> schedules;
    private List<ChatMessage> chats;
    
    public UserData() {
        this.groups = new ArrayList<>();
        this.schedules = new ArrayList<>();
        this.chats = new ArrayList<>();
    }
    
    public UserData(User user, List<Group> groups, List<Schedule> schedules, List<ChatMessage> chats) {
        this.user = user;
        this.groups = groups != null ? groups : new ArrayList<>();
        this.schedules = schedules != null ? schedules : new ArrayList<>();
        this.chats = chats != null ? chats : new ArrayList<>();
    }
    
    // Getters and Setters
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public List<Group> getGroups() { return groups; }
    public void setGroups(List<Group> groups) { this.groups = groups; }
    
    public List<Schedule> getSchedules() { return schedules; }
    public void setSchedules(List<Schedule> schedules) { this.schedules = schedules; }
    
    public List<ChatMessage> getChats() { return chats; }
    public void setChats(List<ChatMessage> chats) { this.chats = chats; }
}
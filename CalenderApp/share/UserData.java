package share;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserData implements Serializable {
    private static final long serialVersionUID = 1L;

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
        this.groups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
        this.schedules = schedules != null ? new ArrayList<>(schedules) : new ArrayList<>();
        this.chats = chats != null ? new ArrayList<>(chats) : new ArrayList<>();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public List<ChatMessage> getChats() {
        return chats;
    }

    public void setChats(List<ChatMessage> chats) {
        this.chats = chats;
    }

    @Override
    public String toString() {
        return "UserData for user: " + (user != null ? user.getUsername() : "null") +
               ", groups: " + groups.size() +
               ", schedules: " + schedules.size() +
               ", chats: " + chats.size();
    }
}
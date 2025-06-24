package share;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description; // GUIの「詳細」に相当
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isAllDay;    // 終日フラグ
    private String groupId;      // 関連するグループのID (個人予定ならnull or 空)
    private List<String> participants; // 参加者のユーザー名リスト
    private String createdBy;    // 作成者のユーザー名
    private boolean isPrivate;   // 非公開フラグ (個人予定のみ有効)

    public Schedule() {
        this.participants = new ArrayList<>();
    }

    // 全てのフィールドを引数に取るコンストラクタ（必要に応じて）
    public Schedule(String id, String title, String description, LocalDateTime startTime, LocalDateTime endTime, boolean isAllDay, String groupId, List<String> participants, String createdBy, boolean isPrivate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAllDay = isAllDay;
        this.groupId = groupId;
        this.participants = participants != null ? new ArrayList<>(participants) : new ArrayList<>();
        this.createdBy = createdBy;
        this.isPrivate = isPrivate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    @Override
    public String toString() {
        return "Schedule{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               ", isAllDay=" + isAllDay +
               ", isPrivate=" + isPrivate +
               '}';
    }
}
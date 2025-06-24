package share;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
// import java.time.LocalDateTime; // createdAt を削除したので不要に

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String createdBy; // グループ作成者のユーザー名
    private List<String> members; // メンバーのユーザー名リスト

    // グループテーマカラーは不要とのことで削除

    public Group() {
        this.members = new ArrayList<>();
    }

    public Group(String id, String name, String createdBy, List<String> members) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    // 必要に応じて equals() や hashCode() を id ベースで実装
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return id != null ? id.equals(group.id) : group.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Group{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", createdBy='" + createdBy + '\'' +
               ", members=" + members +
               '}';
    }
}
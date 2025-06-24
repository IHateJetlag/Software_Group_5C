package share;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupName;
    private List<String> members; // 初期メンバーのユーザー名リスト

    public CreateGroupRequest() {
        this.members = new ArrayList<>();
    }

    public CreateGroupRequest(String groupName, List<String> members) {
        this.groupName = groupName;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
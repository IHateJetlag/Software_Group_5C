package GUI;

import java.awt.Color;
import java.util.Objects;

/**
 * ユーザー情報を保持するデータクラス。
 */
public class User {

    private final String id;
    private final String name;
    private final Color color;
    private final boolean isMe;

    public User(String id, String name, Color color, boolean isMe) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isMe = isMe;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public boolean isMe() {
        return isMe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
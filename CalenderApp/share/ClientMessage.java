package share;

import com.google.gson.JsonElement; // Gsonライブラリのimportが必要
import java.io.Serializable;

public class ClientMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private JsonElement data;

    public ClientMessage() {
    }

    public ClientMessage(String type, JsonElement data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ClientMessage{" +
               "type='" + type + '\'' +
               ", data=" + (data != null ? data.toString().substring(0, Math.min(data.toString().length(), 50))+"..." : "null") +
               '}';
    }
}
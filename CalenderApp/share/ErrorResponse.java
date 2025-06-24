package share;

import java.io.Serializable;

public class ErrorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
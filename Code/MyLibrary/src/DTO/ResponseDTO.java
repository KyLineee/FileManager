package DTO;

import java.io.Serializable;

public class ResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    private final String message;
    private final Object data;

    public ResponseDTO(boolean success, String message) {
        this(success, message, null);
    }

    public ResponseDTO(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
package DTO;

public class DeleteRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String path;

    public DeleteRequest(String path) {
        super(CommandType.DELETE);
        this.path = path;
    }

    public String getPath() { return path; }
}
package DTO;

public class ListFilesRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String relativePath;

    public ListFilesRequest(String relativePath) {
        super(CommandType.LIST_FILES);
        this.relativePath = relativePath;
    }

    public String getRelativePath() { return relativePath; }
}
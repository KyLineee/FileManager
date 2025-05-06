package DTO;

public class CreateFolderRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String folderPath;

    public CreateFolderRequest(String folderPath) {
        super(CommandType.CREATE_FOLDER);
        this.folderPath = folderPath;
    }

    public String getFolderPath() { return folderPath; }
}
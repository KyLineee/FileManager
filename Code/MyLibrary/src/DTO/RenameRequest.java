package DTO;

public class RenameRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String oldPath;
    private final String newName;

    public RenameRequest(String oldPath, String newName) {
        super(CommandType.RENAME);
        this.oldPath = oldPath;
        this.newName = newName;
    }

    public String getOldPath() { return oldPath; }
    public String getNewName() { return newName; }
}

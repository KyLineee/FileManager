package DTO;

public class CopyRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String sourcePath;
    private final String destinationPath;

    public CopyRequest(String sourcePath, String destinationPath) {
        super(CommandType.COPY);
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    public String getSourcePath() { return sourcePath; }
    public String getDestinationPath() { return destinationPath; }
}


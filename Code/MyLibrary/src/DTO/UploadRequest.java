package DTO;

import java.io.Serializable;

public class UploadRequest extends RequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String destinationPath;
    private final long fileSize;
    private final boolean overwrite;

    public UploadRequest(String destinationPath, long fileSize, boolean overwrite) {
        super(CommandType.UPLOAD);
        this.destinationPath = destinationPath;
        this.fileSize = fileSize;
        this.overwrite = overwrite;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public long getFileSize() {
        return fileSize;
    }
    
    public boolean isOverwrite() {
        return overwrite;
    }
}
package DTO; 

import java.io.Serializable;

public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final long size;
    private final long lastModified;
    private final String fileType;

    public FileInfo(String name, long size, long lastModified, String fileType) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.fileType = fileType;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }
    
    public String getFileType() {
        return fileType;
    }
}
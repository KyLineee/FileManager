package DTO;

public class DownloadRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String filePath;

    public DownloadRequest(String filePath) {
        super(CommandType.DOWNLOAD);
        this.filePath = filePath;
    }

    public String getFilePath() { return filePath; }
}

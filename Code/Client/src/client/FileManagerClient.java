package client;

import java.io.*;
import java.net.*;
import java.util.List;
import DTO.*;
import Security.CryptoUtils;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.swing.JOptionPane;

public class FileManagerClient {
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private SecretKey aesKey;
    private PublicKey serverPublicKey;
    
    public FileManagerClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        connect();
    }
    
    private void connect() {
        try {
            socket = new Socket(serverIp, serverPort);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            
            serverPublicKey = CryptoUtils.publicKeyFromString((String) ois.readObject());
            
            aesKey = CryptoUtils.generateAESKey();
            byte[] encryptedAESKey = CryptoUtils.encryptRSA(aesKey.getEncoded(), serverPublicKey);
            oos.writeObject(encryptedAESKey);
            oos.flush();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
     private ResponseDTO sendRequest(RequestDTO request) {
        try {
            byte[] data = serialize(request);
            byte[] encrypted = CryptoUtils.encryptAES(data, aesKey);
            oos.writeObject(encrypted);
            oos.flush();
            
            byte[] encryptedResponse = (byte[]) ois.readObject();
            byte[] decrypted = CryptoUtils.decryptAES(encryptedResponse, aesKey);
            return (ResponseDTO) deserialize(decrypted);
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseDTO(false, "Exception: " + ex.getMessage());
        }
    }
     
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        return bos.toByteArray();
    }

    private Object deserialize(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }
    
    public boolean register(String username, String password) {
        RegisterRequest req = new RegisterRequest(username, password);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    public boolean login(String username, String password) {
        LoginRequest req = new LoginRequest(username, password);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    public boolean uploadFile(File file, String destPath) {
        return uploadFile(file, destPath, false);
    }
    
    private boolean uploadFile(File file, String destPath, boolean overwrite) {
        try {
            UploadRequest req = new UploadRequest(destPath, file.length(), overwrite);
            ResponseDTO resp = sendRequest(req);
            
            if (!resp.isSuccess() && "File conflict".equals(resp.getMessage())) {
                Object[] options = {"Перезаписать", "Переименовать"};
                int choice = JOptionPane.showOptionDialog(null,
                    "Файл с таким именем уже существует. Выберите действие:",
                    "Конфликт файла",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                
                if (choice == JOptionPane.CANCEL_OPTION || choice == -1) {
                    return false;
                }
                
                if (choice == JOptionPane.YES_OPTION) {
                    return uploadFile(file, destPath, true);
                } else if (choice == JOptionPane.NO_OPTION) {
                    File original = new File(destPath);
                    String currentName = original.getName();
                    Object input = JOptionPane.showInputDialog(
                        null,
                        "Введите новое имя файла:",
                        "Переименование",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        currentName
                    );
                    if (input == null || input.toString().trim().isEmpty()) {
                        return false;
                    }

                    String newName = input.toString();
                    String parent = original.getParent();
                    String newDestPath = (parent != null ? parent + File.separator : "") + newName;
                    return uploadFile(file, newDestPath, false);
                }
            } else if (!resp.isSuccess() && !"READY".equals(resp.getMessage())) {
                return false;
            }
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] block = Arrays.copyOf(buffer, bytesRead);
                byte[] encryptedBlock = CryptoUtils.encryptAES(block, aesKey);
                oos.writeObject(encryptedBlock);
            }
            fis.close();
            oos.flush();
            
            byte[] encryptedResponse = (byte[]) ois.readObject();
            byte[] decrypted = CryptoUtils.decryptAES(encryptedResponse, aesKey);
            ResponseDTO finalResp = (ResponseDTO) deserialize(decrypted);

            return finalResp.isSuccess();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    public boolean downloadFile(String filePath, File saveTo) {
        try {
            DownloadRequest req = new DownloadRequest(filePath);
            ResponseDTO resp = sendRequest(req);
            
            if (!resp.isSuccess()) {
                return false;
            }
            
            long fileSize = (Long) resp.getData();
            
            FileOutputStream fos = new FileOutputStream(saveTo);
            byte[] buffer = new byte[4096];
            long remaining = fileSize;
                
            while (remaining > 0) {
                byte[] encryptedBlock = (byte[]) ois.readObject();
                byte[] decrypted = CryptoUtils.decryptAES(encryptedBlock, aesKey);
                
                
                fos.write(decrypted);
                remaining -= decrypted.length;
            }
            
            fos.close();
            return true;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    public boolean createFolder(String folderPath) {
        CreateFolderRequest req = new CreateFolderRequest(folderPath);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    public boolean delete(String path) {
        DeleteRequest req = new DeleteRequest(path);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    public List<FileInfo> search(String query) {
        SearchRequest req = new SearchRequest(query);
        ResponseDTO resp = sendRequest(req);
        if (!resp.isSuccess()) {
            return null;
        }
        return (List<FileInfo>) resp.getData();
    }
    
    public boolean copy(String source, String destination) {
        CopyRequest req = new CopyRequest(source, destination);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    public boolean rename(String oldPath, String newName) {
        RenameRequest req = new RenameRequest(oldPath, newName);
        ResponseDTO resp = sendRequest(req);
        return resp.isSuccess();
    }
    
    // Запрос содержимого папки.
    public List<FileInfo> listFolder(String relativePath) {
        ListFilesRequest req = new ListFilesRequest(relativePath);
        ResponseDTO resp = sendRequest(req);
        if (!resp.isSuccess()) {
            return null;
        }
        return (List<FileInfo>) resp.getData();
    }
}
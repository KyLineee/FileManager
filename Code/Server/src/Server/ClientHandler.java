package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import DTO.*;
import Security.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String username = null;
    private UserDB userDB;
    private SecretKey aesKey;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userDB = new UserDB();
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            oos.writeObject(CryptoUtils.keyToString(Server.getPublicKey()));
            oos.flush();
            
            byte[] encryptedAESKey = (byte[]) ois.readObject();
            byte[] aesKeyBytes = CryptoUtils.decryptRSA(encryptedAESKey, Server.getPrivateKey());
            
            aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            
            while (true) {
                byte[] encryptedRequest = (byte[]) ois.readObject();
                byte[] decrypted = CryptoUtils.decryptAES(encryptedRequest, aesKey);
                Object obj = deserialize(decrypted);
                if (!(obj instanceof RequestDTO)) {
                    sendResponse(new ResponseDTO(false, "Invalid request type"));
                    continue;
                }
                RequestDTO request = (RequestDTO)obj;
                System.out.println("Получена команда: " + request.getCommand());
                if (username == null && 
                   !(request instanceof RegisterRequest) && 
                   !(request instanceof LoginRequest)) {
                    sendResponse(new ResponseDTO(false, "Not authenticated"));
                    continue;
                }
                switch(request.getCommand()){
                    case REGISTER:
                        handleRegister((RegisterRequest) request);
                        break;
                    case LOGIN:
                        handleLogin((LoginRequest) request);
                        break;
                    case UPLOAD:
                        handleUpload((UploadRequest) request);
                        break;
                    case DOWNLOAD:
                        handleDownload((DownloadRequest) request);
                        break;
                    case CREATE_FOLDER:
                        handleCreateFolder((CreateFolderRequest) request);
                        break;
                    case DELETE:
                        handleDelete((DeleteRequest) request);
                        break;
                    case SEARCH:
                        handleSearch((SearchRequest) request);
                        break;
                    case COPY:
                        handleCopy((CopyRequest) request);
                        break;
                    case RENAME:
                        handleRename((RenameRequest) request);
                        break;
                    case LIST_FILES:
                        handleListFiles((ListFilesRequest) request);
                        break;
                    default:
                        sendResponse(new ResponseDTO(false, "Unknown command"));
                }
            }
        } catch (Exception ex) {
            System.out.println("Клиент отключился: " + socket.getInetAddress());
        } finally {
            try { socket.close(); } catch(Exception e) {}
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
    
    private void handleRegister(RegisterRequest req) throws IOException {
        String user = req.getUsername();
        String pass = req.getPassword();
        if (userDB.userExists(user)) {
            sendResponse(new ResponseDTO(false, "User already exists"));
        } else {
            if (userDB.insertUser(user, pass)) {
                username = user;
                new File(Server.getBaseDir() + File.separator + username).mkdirs();
                sendResponse(new ResponseDTO(true, "OK"));
            } else {
                sendResponse(new ResponseDTO(false, "Registration failed"));
            }
        }
    }
    
    private void handleLogin(LoginRequest req) throws IOException {
        String user = req.getUsername();
        String pass = req.getPassword();
        if (userDB.checkPassword(user, pass)) {
            username = user;
            sendResponse(new ResponseDTO(true, "OK"));
        } else {
            sendResponse(new ResponseDTO(false, "Invalid credentials"));
        }
    }


    private File getUserDir() {
        return new File(Server.getBaseDir() + File.separator + username);
    }
    
    private void handleUpload(UploadRequest req) throws IOException, ClassNotFoundException {
        String destPath = req.getDestinationPath();
        long fileSize = req.getFileSize(); // Примечание: фактический размер может отличаться из-за шифрования
        boolean overwrite = req.isOverwrite();
        File outFile = new File(getUserDir(), destPath);
        outFile.getParentFile().mkdirs();

        if (outFile.exists() && !overwrite) {
            sendResponse(new ResponseDTO(false, "File conflict"));
            return;
        }

        sendResponse(new ResponseDTO(true, "READY"));

        try (FileOutputStream fos = new FileOutputStream(outFile, false)) {
            long remaining = fileSize;

            while (true) {
                byte[] encryptedBlock = (byte[]) ois.readObject();
                byte[] decryptedBlock;
                try {
                    decryptedBlock = CryptoUtils.decryptAES(encryptedBlock, aesKey);
                } catch (Exception e) {
                    sendResponse(new ResponseDTO(false, "Decryption error"));
                    return;
                }

                fos.write(decryptedBlock);
                remaining -= decryptedBlock.length;
                if (remaining <= 0) break;
            }

            sendResponse(new ResponseDTO(true, "OK"));
        } catch (Exception ex) {
            sendResponse(new ResponseDTO(false, "Upload failed: " + ex.getMessage()));
            throw ex;
        }
    }
    
    private void handleDownload(DownloadRequest req) throws IOException {
        String filePath = req.getFilePath();
        File file = new File(getUserDir(), filePath);

        if (!file.exists() || file.isDirectory()) {
            sendResponse(new ResponseDTO(false, "File not found"));
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            sendResponse(new ResponseDTO(true, "OK", fileSize));

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] blockToEncrypt = Arrays.copyOf(buffer, bytesRead);
                byte[] encryptedBlock = CryptoUtils.encryptAES(blockToEncrypt, aesKey);

                oos.writeObject(encryptedBlock);
            }

            oos.flush();
        } catch (Exception ex) {
            sendResponse(new ResponseDTO(false, "Download failed: " + ex.getMessage()));
        }
    }
    
    private void handleCreateFolder(CreateFolderRequest req) throws IOException {
        String folderPath = req.getFolderPath();
        File folder = new File(getUserDir(), folderPath);
        boolean created = folder.mkdirs();
        sendResponse(new ResponseDTO(created, created ? "OK" : "Unable to create folder"));
    }
    
     private void handleDelete(DeleteRequest req) throws IOException {
        String path = req.getPath();
        File file = new File(getUserDir(), path);
        boolean deleted = deleteRecursively(file);
        sendResponse(new ResponseDTO(deleted, deleted ? "OK" : "Deletion failed"));
    }
    
    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
    
    private FileInfo toFileInfo(File f, int baseLength) {
        String fileType;
        if (f.isDirectory()) {
            fileType = "Папка";
        } else {
            int dot = f.getName().lastIndexOf(".");
            if (dot != -1) {
                fileType = f.getName().substring(dot + 1).toLowerCase();
            } else {
                fileType = "Файл";
            }
        }
        long size = f.isDirectory() ? 0 : f.length();
        String name = (baseLength >= 0) ? f.getAbsolutePath().substring(baseLength + 1) : f.getName();
        return new FileInfo(name, size, f.lastModified(), fileType);
    }
    
    private void handleSearch(SearchRequest req) throws IOException {
        String query = req.getQuery();
        File baseDir = getUserDir();
        List<FileInfo> results = new ArrayList<>();
        int baseLength = baseDir.getAbsolutePath().length();
        searchFiles(baseDir, query, results, baseLength);
        sendResponse(new ResponseDTO(true, "OK", results));
    }

    private void searchFiles(File dir, String query, List<FileInfo> results, int baseLength) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.getName().contains(query)) {
                results.add(toFileInfo(f, baseLength));
            }
            if (f.isDirectory()) {
                searchFiles(f, query, results, baseLength);
            }
        }
    }
    
    private void handleCopy(CopyRequest req) throws IOException {
        String sourcePath = req.getSourcePath();
        String destPath = req.getDestinationPath();
        File source = new File(getUserDir(), sourcePath);
        File dest = new File(getUserDir(), destPath);
        
        if (dest.exists()) {
            dest = getAvailableName(dest);
        }

        boolean success = false;
        if (source.isDirectory()) {
            success = copyDirectory(source, dest);
        } else if (source.isFile()) {
            dest.getParentFile().mkdirs();
            success = copyFile(source, dest);
        }

        sendResponse(new ResponseDTO(success, success ? "Copied as: " + dest.getName() : "Copy failed"));
    }
    
    private File getAvailableName(File original) {
        String name = original.getName();
        File parent = original.getParentFile();

        String baseName;
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (original.isFile() && dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        } else {
            baseName = name;
        }

        int index = 1;
        File candidate;
        do {
            String newName = baseName + " (" + index + ")" + extension;
            candidate = new File(parent, newName);
            index++;
        } while (candidate.exists());

        return candidate;
    }
    
    private boolean copyFile(File source, File dest) {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean copyDirectory(File source, File dest) {
        if (!dest.exists())
            dest.mkdirs();
        File[] files = source.listFiles();
        if (files == null)
            return false;
        for (File file : files) {
            File destFile = new File(dest, file.getName());
            if (file.isDirectory()) {
                if (!copyDirectory(file, destFile))
                    return false;
            } else {
                if (!copyFile(file, destFile))
                    return false;
            }
        }
        return true;
    }
    
    private void handleRename(RenameRequest req) throws IOException {
        String oldPath = req.getOldPath();
        String newName = req.getNewName();
        File oldFile = new File(getUserDir(), oldPath);
        File newFile = new File(oldFile.getParentFile(), newName);
        boolean renamed = oldFile.renameTo(newFile);
        sendResponse(new ResponseDTO(renamed, renamed ? "OK" : "Rename failed"));
    }
    
    private void handleListFiles(ListFilesRequest req) throws IOException {
        String relativePath = req.getRelativePath();
        File folder = new File(getUserDir(), relativePath);
        if (!folder.exists() || !folder.isDirectory()) {
            sendResponse(new ResponseDTO(false, "Folder not found"));
            return;
        }
        File[] files = folder.listFiles();
        List<FileInfo> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                list.add(toFileInfo(f, -1));
            }
        }
        sendResponse(new ResponseDTO(true, "OK", list));
    }
    
    private void sendResponse(ResponseDTO response) throws IOException {
        try {
            byte[] data = serialize(response);
            byte[] encrypted = CryptoUtils.encryptAES(data, aesKey);
            oos.writeObject(encrypted);
            oos.flush();
        } catch (Exception e) {
            throw new IOException("Encryption failed", e);
        }
    }
}
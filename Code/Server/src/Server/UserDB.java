package Server;

import java.io.File;
import java.sql.*;
import org.mindrot.BCrypt;

public class UserDB {
    private static final String BASE_DIR = "server_data/users";
    private static final String DB_URL = "jdbc:sqlite:server_data/users/users.db";

    public UserDB() {
        new File(BASE_DIR).mkdirs();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                String createTable = "CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY," +
                        "password TEXT NOT NULL" +
                        ");";
                Statement stmt = conn.createStatement();
                stmt.execute(createTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean userExists(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertUser(String username, String password) {
        String query = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            // Генерируем хэш пароля с BCrypt. Второй параметр - число раундов.
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkPassword(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                // Сравниваем введённый пароль с хэшем
                return BCrypt.checkpw(password, storedHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
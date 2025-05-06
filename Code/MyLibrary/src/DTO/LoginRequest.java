package DTO;

public class LoginRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        super(CommandType.LOGIN);
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
package DTO;

public class RegisterRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;

    public RegisterRequest(String username, String password) {
        super(CommandType.REGISTER);
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
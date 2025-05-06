package client;

import javax.swing.SwingUtilities;

public class ClientApp {
    public static void main(String[] args) {
        FileManagerClient client = new FileManagerClient("127.0.0.1", 12345);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginFrame loginFrame = new LoginFrame(client);
                loginFrame.setTitle("Вход в файловый менеджер");
                loginFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
                loginFrame.setVisible(true);
            }
        });
    }
}
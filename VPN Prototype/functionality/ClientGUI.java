package functionality;

import java.awt.*;
import java.util.List;
import javax.swing.*;

public class ClientGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("VPN Authentication");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 230);
        frame.setLayout(new GridBagLayout());

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);

        JButton loginButton = new JButton("Login");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(userLabel, gbc);

        gbc.gridx = 1;
        frame.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(passLabel, gbc);

        gbc.gridx = 1;
        frame.add(passField, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 2;
        frame.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter both username and password.",
                        "Input Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Client.retrySend(username, password);
                JOptionPane.showMessageDialog(frame,
                        "Authentication successful.",
                        "Login Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                NetUtils.logClient("GUI login error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame,
                        "Login failed: " + ex.getMessage(),
                        "Authentication Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Autofill with last known user
        List<String> history = Client.loadLoginHistory();
        if (!history.isEmpty()) {
            userField.setText(history.get(history.size() - 1));
        }
    }
}

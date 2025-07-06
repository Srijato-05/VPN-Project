package functionality;

import java.awt.*;
import javax.swing.*;

public class ClientGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("VPN Authentication");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLayout(new GridBagLayout());

        // UI Components
        JLabel labelUsername = new JLabel("Username:");
        JLabel labelPassword = new JLabel("Password:");
        JComboBox<String> userDropdown = new JComboBox<>(Client.loadLoginHistory().toArray(new String[0]));
        JTextField newUserField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Dropdown
        gbc.gridx = 0; gbc.gridy = 0;
        frame.add(new JLabel("Select Previous:"), gbc);
        gbc.gridx = 1;
        frame.add(userDropdown, gbc);

        // Row 2: New Username
        gbc.gridx = 0; gbc.gridy = 1;
        frame.add(labelUsername, gbc);
        gbc.gridx = 1;
        frame.add(newUserField, gbc);

        // Row 3: Password
        gbc.gridx = 0; gbc.gridy = 2;
        frame.add(labelPassword, gbc);
        gbc.gridx = 1;
        frame.add(passwordField, gbc);

        // Row 4: Login Button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        frame.add(loginButton, gbc);

        // Login button action
        loginButton.addActionListener(e -> {
            String typedUser = newUserField.getText().trim();
            String selectedUser = (String) userDropdown.getSelectedItem();
            String username = typedUser.isEmpty() ? selectedUser : typedUser;
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter username and password.",
                        "Input Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String response = Client.sendCredentials(username, password);

                // Show result popup
                if (response.toLowerCase().contains("success")) {
                    JOptionPane.showMessageDialog(frame,
                            response,
                            "Login Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    Client.saveToHistory(username);
                } else {
                    JOptionPane.showMessageDialog(frame,
                            response,
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                NetUtils.logClient("GUI error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame,
                        "Error connecting to server:\n" + ex.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setLocationRelativeTo(null); // center
        frame.setVisible(true);
    }
}

package functionality;

import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.swing.*;

public class ClientGUI {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CONFIG_BASE = "openvpn-configs";
    private static final String AUTH_FILE = "vpn-auth.txt";
    private static Process vpnProcess = null;
    private static JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("VPN Authentication");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 400);
        frame.setLayout(new GridBagLayout());

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);

        JLabel platformLabel = new JLabel("Select Platform:");
        String[] platforms = {"android", "ios", "linux", "macos", "windows"};
        JComboBox<String> platformDropdown = new JComboBox<>(platforms);

        JLabel configLabel = new JLabel("VPN Config File:");
        JComboBox<String> configDropdown = new JComboBox<>();

        JButton connectButton = new JButton("Connect VPN");
        JButton disconnectButton = new JButton("Disconnect VPN");

        JLabel timestampLabel = new JLabel("Timestamp: " + LocalDateTime.now().format(FORMATTER));
        JLabel note = new JLabel("Authentication includes secure timestamp");

        statusLabel = new JLabel("Status: Disconnected");

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

        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(platformLabel, gbc);
        gbc.gridx = 1;
        frame.add(platformDropdown, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        frame.add(configLabel, gbc);
        gbc.gridx = 1;
        frame.add(configDropdown, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        frame.add(timestampLabel, gbc);

        gbc.gridy = 5;
        frame.add(note, gbc);

        gbc.gridy = 6;
        frame.add(connectButton, gbc);

        gbc.gridy = 7;
        frame.add(disconnectButton, gbc);

        gbc.gridy = 8;
        frame.add(statusLabel, gbc);

        // Platform dropdown loads config files
        platformDropdown.addActionListener(e -> {
            String selected = Objects.requireNonNull(platformDropdown.getSelectedItem()).toString();
            File configDir = new File(CONFIG_BASE + File.separator + selected);
            configDropdown.removeAllItems();
            if (configDir.exists() && configDir.isDirectory()) {
                for (File f : Objects.requireNonNull(configDir.listFiles((d, name) -> name.endsWith(".ovpn")))) {
                    configDropdown.addItem(f.getName());
                }
            }
        });

        platformDropdown.setSelectedIndex(0);  // initialize config

        connectButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            String platform = Objects.requireNonNull(platformDropdown.getSelectedItem()).toString();
            String configFile = (String) configDropdown.getSelectedItem();

            if (username.isEmpty() || password.isEmpty() || configFile == null) {
                JOptionPane.showMessageDialog(frame, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                File authFile = new File(AUTH_FILE);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(authFile))) {
                    writer.write(username);
                    writer.newLine();
                    writer.write(password);
                }

                File config = new File(CONFIG_BASE + File.separator + platform + File.separator + configFile);
                connectOpenVPN(config, authFile);

                timestampLabel.setText("Timestamp: " + LocalDateTime.now().format(FORMATTER));
            } catch (Exception ex) {
                NetUtils.logClient("[GUI] VPN Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, "VPN failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        disconnectButton.addActionListener(e -> {
            if (vpnProcess != null && vpnProcess.isAlive()) {
                vpnProcess.destroy();
                NetUtils.logClient("[VPN] Disconnected.");
                statusLabel.setText("Status: Disconnected");
                JOptionPane.showMessageDialog(frame, "VPN disconnected.", "Disconnected", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "VPN is not running.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

        private static void connectOpenVPN(File selectedFile, File authFile) throws IOException {
        String openvpnExe = "C:\\Program Files\\OpenVPN\\bin\\openvpn.exe";

        String configPath = selectedFile.getAbsolutePath();
        String authPath = authFile.getAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(
                openvpnExe,
                "--config", configPath,
                "--auth-user-pass", authPath
        );

        builder.redirectErrorStream(true);
        vpnProcess = builder.start();
        statusLabel.setText("Status: Connecting...");

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(vpnProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[VPN] " + line);
                    NetUtils.logClient("[VPN] " + line);
                    if (line.contains("Initialization Sequence Completed")) {
                        statusLabel.setText("Status: Connected");
                    }
                    if (line.contains("SIGTERM") || line.contains("AUTH_FAILED")) {
                        statusLabel.setText("Status: Disconnected");
                    }
                }
            } catch (IOException e) {
                NetUtils.logClient("[VPN] Output error: " + e.getMessage());
            }
        }).start();
    }
}

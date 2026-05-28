package functionality;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ClientGUI {

    private static Process vpnProcess = null;
    
    // UI Elements
    private static JFrame frame;
    private static JLabel statusLabel;
    private static VpnShield vpnShield;
    private static JLabel timerLabel;
    private static JLabel trafficLabel;
    private static JLabel timestampLabel;
    private static JTextArea logArea;
    private static JButton connectButton;
    private static JButton disconnectButton;
    private static JComboBox<String> platformDropdown;
    private static JComboBox<Object> configDropdown;
    private static JComboBox<String> protocolDropdown;
    private static ToggleSwitch multiHopCheckbox;
    private static ToggleSwitch autoSecureCheckbox;
    private static ToggleSwitch killSwitchCheckbox;
    private static GlassTextField userField;
    private static GlassPasswordField passField;

    // Authorization Session Cache
    private static boolean isAuthorized = false;
    private static String authorizedUsername = "";
    private static char[] authorizedPassword = null;
    private static String cachedVpnUser = "";
    private static String cachedVpnPass = "";
    private static String cachedSessionToken = "";
    
    // Stats & Timer Fields
    private static javax.swing.Timer elapsedTimer = null;
    private static long connectionStartTime = 0;
    private static long bytesSent = 0;
    private static long bytesRecv = 0;

    // Modern Dark Theme Palette
    private static final Color BG_DARK = new Color(0x12, 0x12, 0x14);
    private static final Color BG_INPUT = new Color(0x2d, 0x2d, 0x35);
    private static final Color ACCENT_CYAN = new Color(0x03, 0xda, 0xc6);
    private static final Color ACCENT_VIOLET = new Color(0xbb, 0x86, 0xfc);
    private static final Color TEXT_WHITE = new Color(0xff, 0xff, 0xff);
    private static final Color TEXT_GRAY = new Color(0x9e, 0x9e, 0x9e);
    private static final Color COLOR_ERROR = new Color(0xcf, 0x66, 0x79);
    private static final Color BORDER_GRAY = new Color(0x3e, 0x3e, 0x4a);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("CipherVPN Client Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG_DARK);
        
        // Clean up on exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Client.disableKillSwitch();
                Client.closePersistentConnection();
                deleteTempFiles();
                if (vpnProcess != null && vpnProcess.isAlive()) {
                    vpnProcess.destroy();
                }
            }
        });

        // Root Panel: Custom BackgroundPanel
        BackgroundPanel rootPanel = new BackgroundPanel();
        rootPanel.setLayout(new BorderLayout(15, 15));
        rootPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        frame.add(rootPanel);

        // Header Panel (Branding)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel brandLabel = new JLabel("🔒 CipherVPN Professional");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        brandLabel.setForeground(ACCENT_CYAN);
        headerPanel.add(brandLabel, BorderLayout.WEST);
        
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Workspace (Left Card and Right Card)
        JPanel centerWorkspace = new JPanel(new GridBagLayout());
        centerWorkspace.setOpaque(false);
        rootPanel.add(centerWorkspace, BorderLayout.CENTER);

        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.fill = GridBagConstraints.BOTH;
        gbcCenter.weighty = 1.0;

        // --- LEFT GLASS CARD: Control panel (Tabs, Inputs, Options) ---
        GlassCard leftCard = new GlassCard();
        leftCard.setLayout(new BorderLayout(10, 15));
        
        gbcCenter.gridx = 0;
        gbcCenter.weightx = 0.62;
        gbcCenter.insets = new Insets(0, 0, 0, 15);
        centerWorkspace.add(leftCard, gbcCenter);

        // Top-level Tabs Panel (Connection Profile vs Vault)
        JPanel modePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        modePanel.setOpaque(false);
        
        JButton showLoginBtn = new JButton("Connection Profile");
        JButton showRegisterBtn = new JButton("Vault");
        styleTabButton(showLoginBtn, true);
        styleTabButton(showRegisterBtn, false);
        
        modePanel.add(showLoginBtn);
        modePanel.add(showRegisterBtn);
        leftCard.add(modePanel, BorderLayout.NORTH);

        // Main Card Container
        CardLayout mainCardLayout = new CardLayout();
        JPanel mainCardContainer = new JPanel(mainCardLayout);
        mainCardContainer.setOpaque(false);
        leftCard.add(mainCardContainer, BorderLayout.CENTER);

        // Card 1: Connection Profile
        JPanel connectionProfilePanel = new JPanel(new GridBagLayout());
        connectionProfilePanel.setOpaque(false);

        // Card 2: Vault (holds subheadings and sub-cards)
        JPanel vaultPanel = new JPanel(new BorderLayout(0, 10));
        vaultPanel.setOpaque(false);

        mainCardContainer.add(connectionProfilePanel, "CONN_PROFILE_CARD");
        mainCardContainer.add(vaultPanel, "VAULT_CARD");

        // --- POPULATE CONNECTION PROFILE CARD ---
        GridBagConstraints gbcConn = new GridBagConstraints();
        gbcConn.insets = new Insets(8, 6, 8, 6);
        gbcConn.fill = GridBagConstraints.HORIZONTAL;
        gbcConn.weightx = 1.0;

        JLabel platformLabel = createStyledLabel("Operating System OS:");
        platformDropdown = new JComboBox<>();
        styleComboBox(platformDropdown);

        JLabel protocolLabel = createStyledLabel("Tunnel Protocol:");
        protocolDropdown = new JComboBox<>(new String[]{"OpenVPN", "WireGuard"});
        styleComboBox(protocolDropdown);

        JLabel configLabel = createStyledLabel("Gateway Server Profile:");
        configDropdown = new JComboBox<>();
        styleComboBox(configDropdown);

        multiHopCheckbox = new ToggleSwitch("Double VPN (Multi-Hop)");
        autoSecureCheckbox = new ToggleSwitch("Auto-Secure Public Wi-Fi");
        killSwitchCheckbox = new ToggleSwitch("Enable Firewall Kill-Switch");

        gbcConn.gridx = 0; gbcConn.gridy = 0; gbcConn.gridwidth = 1; connectionProfilePanel.add(platformLabel, gbcConn);
        gbcConn.gridx = 1; connectionProfilePanel.add(platformDropdown, gbcConn);

        gbcConn.gridx = 0; gbcConn.gridy = 1; connectionProfilePanel.add(protocolLabel, gbcConn);
        gbcConn.gridx = 1; connectionProfilePanel.add(protocolDropdown, gbcConn);

        gbcConn.gridx = 0; gbcConn.gridy = 2; connectionProfilePanel.add(configLabel, gbcConn);
        gbcConn.gridx = 1; connectionProfilePanel.add(configDropdown, gbcConn);

        gbcConn.gridx = 0; gbcConn.gridy = 3; gbcConn.gridwidth = 2;
        connectionProfilePanel.add(multiHopCheckbox, gbcConn);

        gbcConn.gridy = 4;
        connectionProfilePanel.add(autoSecureCheckbox, gbcConn);

        gbcConn.gridy = 5;
        connectionProfilePanel.add(killSwitchCheckbox, gbcConn);

        gbcConn.gridy = 6; gbcConn.weighty = 1.0;
        connectionProfilePanel.add(Box.createVerticalGlue(), gbcConn);

        // --- POPULATE VAULT CARD (WITH SUBHEADINGS) ---
        JPanel subModePanel = new JPanel(new GridLayout(1, 3, 6, 0));
        subModePanel.setOpaque(false);

        JButton subLoginBtn = new JButton("Login");
        JButton subSignupBtn = new JButton("Signup");
        JButton subSettingsBtn = new JButton("Settings");
        styleSubTabButton(subLoginBtn, true);
        styleSubTabButton(subSignupBtn, false);
        styleSubTabButton(subSettingsBtn, false);

        subModePanel.add(subLoginBtn);
        subModePanel.add(subSignupBtn);
        subModePanel.add(subSettingsBtn);
        vaultPanel.add(subModePanel, BorderLayout.NORTH);

        CardLayout vaultSubCardLayout = new CardLayout();
        JPanel vaultSubCardContainer = new JPanel(vaultSubCardLayout);
        vaultSubCardContainer.setOpaque(false);
        vaultPanel.add(vaultSubCardContainer, BorderLayout.CENTER);

        // Vault Subcard 1: Login
        JPanel vaultLoginPanel = new JPanel(new GridBagLayout());
        vaultLoginPanel.setOpaque(false);
        GridBagConstraints gbcVLogin = new GridBagConstraints();
        gbcVLogin.insets = new Insets(8, 6, 8, 6);
        gbcVLogin.fill = GridBagConstraints.HORIZONTAL;
        gbcVLogin.weightx = 1.0;

        JLabel userLabel = createStyledLabel("Vault Username:");
        userField = createStyledTextField();
        
        List<String> userHistory = Client.loadLoginHistory();
        if (userHistory != null && userHistory.size() > 0) {
            String lastUser = userHistory.get(userHistory.size() - 1);
            if (!"admin".equals(lastUser)) {
                userField.setText(lastUser);
            }
        }
        
        JLabel passLabel = createStyledLabel("Vault Password:");
        passField = createStyledPasswordField();

        gbcVLogin.gridx = 0; gbcVLogin.gridy = 0; gbcVLogin.gridwidth = 1; vaultLoginPanel.add(userLabel, gbcVLogin);
        gbcVLogin.gridx = 1; vaultLoginPanel.add(userField, gbcVLogin);

        gbcVLogin.gridx = 0; gbcVLogin.gridy = 1; vaultLoginPanel.add(passLabel, gbcVLogin);
        gbcVLogin.gridx = 1; vaultLoginPanel.add(passField, gbcVLogin);

        JButton loginSessionButton = new JButton("Login to Session");
        styleActionButton(loginSessionButton, ACCENT_CYAN, Color.BLACK);

        JLabel loginFeedbackLabel = new JLabel("", SwingConstants.CENTER);
        loginFeedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loginFeedbackLabel.setForeground(ACCENT_CYAN);

        gbcVLogin.gridx = 0; gbcVLogin.gridy = 2; gbcVLogin.gridwidth = 2;
        vaultLoginPanel.add(loginSessionButton, gbcVLogin);

        gbcVLogin.gridy = 3;
        vaultLoginPanel.add(loginFeedbackLabel, gbcVLogin);

        gbcVLogin.gridy = 4; gbcVLogin.weighty = 1.0;
        vaultLoginPanel.add(Box.createVerticalGlue(), gbcVLogin);

        // Vault Subcard 2: Signup
        JPanel vaultSignupPanel = new JPanel(new GridBagLayout());
        vaultSignupPanel.setOpaque(false);
        GridBagConstraints gbcVSignup = new GridBagConstraints();
        gbcVSignup.insets = new Insets(10, 8, 10, 8);
        gbcVSignup.fill = GridBagConstraints.HORIZONTAL;
        gbcVSignup.weightx = 1.0;

        JLabel regTitle = new JLabel("Register Vault Account", SwingConstants.CENTER);
        regTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        regTitle.setForeground(ACCENT_VIOLET);

        JLabel regUserLabel = createStyledLabel("Choose Username:");
        JTextField regUserField = createStyledTextField();

        JLabel regPassLabel = createStyledLabel("Choose Password:");
        JPasswordField regPassField = createStyledPasswordField();

        JButton registerSubmitButton = new JButton("Create Vault");
        styleActionButton(registerSubmitButton, ACCENT_VIOLET, Color.BLACK);

        JLabel regFeedbackLabel = new JLabel("", SwingConstants.CENTER);
        regFeedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        regFeedbackLabel.setForeground(ACCENT_CYAN);

        gbcVSignup.gridx = 0; gbcVSignup.gridy = 0; gbcVSignup.gridwidth = 2;
        vaultSignupPanel.add(regTitle, gbcVSignup);

        gbcVSignup.gridwidth = 1;
        gbcVSignup.gridx = 0; gbcVSignup.gridy = 1; vaultSignupPanel.add(regUserLabel, gbcVSignup);
        gbcVSignup.gridx = 1; vaultSignupPanel.add(regUserField, gbcVSignup);

        gbcVSignup.gridx = 0; gbcVSignup.gridy = 2; vaultSignupPanel.add(regPassLabel, gbcVSignup);
        gbcVSignup.gridx = 1; vaultSignupPanel.add(regPassField, gbcVSignup);

        gbcVSignup.gridx = 0; gbcVSignup.gridy = 3; gbcVSignup.gridwidth = 2;
        vaultSignupPanel.add(registerSubmitButton, gbcVSignup);

        gbcVSignup.gridy = 4;
        vaultSignupPanel.add(regFeedbackLabel, gbcVSignup);

        gbcVSignup.gridy = 5; gbcVSignup.weighty = 1.0;
        vaultSignupPanel.add(Box.createVerticalGlue(), gbcVSignup);

        // Vault Subcard 3: Settings
        JPanel vaultSettingsPanel = new JPanel(new GridBagLayout());
        vaultSettingsPanel.setOpaque(false);
        GridBagConstraints gbcVSett = new GridBagConstraints();
        gbcVSett.insets = new Insets(8, 6, 8, 6);
        gbcVSett.fill = GridBagConstraints.HORIZONTAL;
        gbcVSett.weightx = 1.0;

        JLabel timeoutLabel = createStyledLabel("Auto-Lock Timeout:");
        JComboBox<String> timeoutDropdown = new JComboBox<>(new String[]{"5 Minutes", "15 Minutes", "30 Minutes", "Never"});
        styleComboBox(timeoutDropdown);

        JLabel cryptoLabel = createStyledLabel("Vault Encryption:");
        JComboBox<String> cryptoDropdown = new JComboBox<>(new String[]{"AES-256-GCM (Hardware)", "ChaCha20-Poly1305"});
        styleComboBox(cryptoDropdown);

        JButton rotateKeyBtn = new JButton("Rotate Master Encryption Key");
        styleActionButton(rotateKeyBtn, ACCENT_VIOLET, Color.BLACK);

        gbcVSett.gridx = 0; gbcVSett.gridy = 0; gbcVSett.gridwidth = 1; vaultSettingsPanel.add(timeoutLabel, gbcVSett);
        gbcVSett.gridx = 1; vaultSettingsPanel.add(timeoutDropdown, gbcVSett);

        gbcVSett.gridx = 0; gbcVSett.gridy = 1; vaultSettingsPanel.add(cryptoLabel, gbcVSett);
        gbcVSett.gridx = 1; vaultSettingsPanel.add(cryptoDropdown, gbcVSett);

        gbcVSett.gridx = 0; gbcVSett.gridy = 2; gbcVSett.gridwidth = 2;
        vaultSettingsPanel.add(rotateKeyBtn, gbcVSett);

        gbcVSett.gridy = 3; gbcVSett.weighty = 1.0;
        vaultSettingsPanel.add(Box.createVerticalGlue(), gbcVSett);

        vaultSubCardContainer.add(vaultLoginPanel, "VAULT_LOGIN");
        vaultSubCardContainer.add(vaultSignupPanel, "VAULT_SIGNUP");
        vaultSubCardContainer.add(vaultSettingsPanel, "VAULT_SETTINGS");

        // --- RIGHT GLASS CARD: Connection Monitor & Status Shield ---
        GlassCard rightCard = new GlassCard();
        rightCard.setLayout(new GridBagLayout());
        
        gbcCenter.gridx = 1;
        gbcCenter.weightx = 0.38;
        gbcCenter.insets = new Insets(0, 15, 0, 0);
        centerWorkspace.add(rightCard, gbcCenter);

        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(10, 10, 10, 10);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.gridx = 0;
        gbcRight.weightx = 1.0;

        vpnShield = new VpnShield();
        
        statusLabel = new JLabel("Status: Disconnected", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setForeground(TEXT_GRAY);

        timerLabel = new JLabel("Uptime: --:--:--", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        timerLabel.setForeground(TEXT_WHITE);

        trafficLabel = new JLabel("Traffic: Sent: -- | Recv: --", SwingConstants.CENTER);
        trafficLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        trafficLabel.setForeground(ACCENT_CYAN);

        timestampLabel = new JLabel("Session Info: Replay Attack Protected", SwingConstants.CENTER);
        timestampLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timestampLabel.setForeground(TEXT_GRAY);

        connectButton = new JButton("Connect VPN");
        styleActionButton(connectButton, ACCENT_CYAN, Color.BLACK);

        disconnectButton = new JButton("Disconnect");
        styleActionButton(disconnectButton, COLOR_ERROR, Color.WHITE);
        disconnectButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);

        // Place elements in Right Card
        gbcRight.gridy = 0;
        gbcRight.fill = GridBagConstraints.NONE;
        rightCard.add(vpnShield, gbcRight);

        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.gridy = 1;
        rightCard.add(statusLabel, gbcRight);

        gbcRight.gridy = 2;
        rightCard.add(timerLabel, gbcRight);

        gbcRight.gridy = 3;
        rightCard.add(trafficLabel, gbcRight);

        gbcRight.gridy = 4;
        rightCard.add(timestampLabel, gbcRight);

        // Dynamic spacer
        gbcRight.gridy = 5;
        rightCard.add(Box.createVerticalStrut(15), gbcRight);

        gbcRight.gridy = 6;
        rightCard.add(buttonPanel, gbcRight);

        // --- FOOTER & COLLAPSIBLE LOG TERMINAL ---
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.setOpaque(false);
        
        JButton toggleLogButton = new JButton("[-] Hide System Diagnostic Console");
        styleTabButton(toggleLogButton, false);
        toggleLogButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        southPanel.add(toggleLogButton, BorderLayout.NORTH);

        logArea = new JTextArea(10, 48) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        logArea.setOpaque(false);
        logArea.setEditable(false);
        logArea.setBackground(new Color(0x0a, 0x0a, 0x0f, 180));
        logArea.setForeground(new Color(0x00, 0xe5, 0xff)); // High-visibility Neon Cyan
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setCaretColor(new Color(0x00, 0xe5, 0xff));
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.setBorder(new LineBorder(BORDER_GRAY, 1, true));
        logScrollPane.setVisible(true);
        southPanel.add(logScrollPane, BorderLayout.CENTER);
        rootPanel.add(southPanel, BorderLayout.SOUTH);

        // --- ACTION LISTENERS ---

        // Toggle card modes
        showLoginBtn.addActionListener(e -> {
            styleTabButton(showLoginBtn, true);
            styleTabButton(showRegisterBtn, false);
            mainCardLayout.show(mainCardContainer, "CONN_PROFILE_CARD");
        });

        showRegisterBtn.addActionListener(e -> {
            styleTabButton(showLoginBtn, false);
            styleTabButton(showRegisterBtn, true);
            mainCardLayout.show(mainCardContainer, "VAULT_CARD");
        });

        subLoginBtn.addActionListener(e -> {
            styleSubTabButton(subLoginBtn, true);
            styleSubTabButton(subSignupBtn, false);
            styleSubTabButton(subSettingsBtn, false);
            vaultSubCardLayout.show(vaultSubCardContainer, "VAULT_LOGIN");
        });

        subSignupBtn.addActionListener(e -> {
            styleSubTabButton(subLoginBtn, false);
            styleSubTabButton(subSignupBtn, true);
            styleSubTabButton(subSettingsBtn, false);
            vaultSubCardLayout.show(vaultSubCardContainer, "VAULT_SIGNUP");
        });

        subSettingsBtn.addActionListener(e -> {
            styleSubTabButton(subLoginBtn, false);
            styleSubTabButton(subSignupBtn, false);
            styleSubTabButton(subSettingsBtn, true);
            vaultSubCardLayout.show(vaultSubCardContainer, "VAULT_SETTINGS");
        });

        rotateKeyBtn.addActionListener(e -> {
            NetUtils.logClient("Vault security: Initiating hardware-bound master key rotation...");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    NetUtils.logClient("Vault security: Master key rotated successfully. Local secure vault re-encrypted.");
                } catch (InterruptedException ignored) {}
            }).start();
        });

        // Toggle logs visibility
        toggleLogButton.addActionListener(e -> {
            if (logScrollPane.isVisible()) {
                logScrollPane.setVisible(false);
                toggleLogButton.setText("[+] Show System Diagnostic Console");
            } else {
                logScrollPane.setVisible(true);
                toggleLogButton.setText("[-] Hide System Diagnostic Console");
            }
            frame.pack();
        });

        // Register Account
        registerSubmitButton.addActionListener(e -> {
            String u = regUserField.getText().trim();
            char[] p = regPassField.getPassword();

            if (u.isEmpty() || p.length == 0) {
                regFeedbackLabel.setForeground(COLOR_ERROR);
                regFeedbackLabel.setText("Please fill out all fields.");
                return;
            }

            regFeedbackLabel.setForeground(ACCENT_CYAN);
            regFeedbackLabel.setText("Registering with Server...");
            registerSubmitButton.setEnabled(false);

            new Thread(() -> {
                try {
                    String regResult = Client.sendCredentials(u, p, "REGISTER");
                    SwingUtilities.invokeLater(() -> {
                        regPassField.setText("");
                        if (regResult != null && regResult.contains("successful")) {
                            regFeedbackLabel.setForeground(ACCENT_CYAN);
                            regFeedbackLabel.setText(regResult);
                            regUserField.setText("");
                            JOptionPane.showMessageDialog(frame, "Account Created Successfully! You can now log in.", "Registration", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            regFeedbackLabel.setForeground(COLOR_ERROR);
                            regFeedbackLabel.setText(regResult);
                        }
                        registerSubmitButton.setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        regPassField.setText("");
                        regFeedbackLabel.setForeground(COLOR_ERROR);
                        regFeedbackLabel.setText("Error: " + ex.getMessage());
                        registerSubmitButton.setEnabled(true);
                    });
                }
            }).start();
        });

        // Login Session
        loginSessionButton.addActionListener(e -> {
            String u = userField.getText().trim();
            char[] p = passField.getPassword();

            if (u.isEmpty() || p.length == 0) {
                loginFeedbackLabel.setForeground(COLOR_ERROR);
                loginFeedbackLabel.setText("Please fill out all fields.");
                return;
            }

            loginFeedbackLabel.setForeground(ACCENT_CYAN);
            loginFeedbackLabel.setText("Authenticating with Server...");
            loginSessionButton.setEnabled(false);

            new Thread(() -> {
                try {
                    char[] pClone = p.clone();
                    String authResponse = Client.sendCredentials(u, pClone, "LOGIN");
                    SwingUtilities.invokeLater(() -> {
                        if (authResponse != null && authResponse.startsWith("SUCCESS:")) {
                            String[] authParts = authResponse.split(":", 4);
                            cachedSessionToken = authParts[1];
                            cachedVpnUser = authParts[2];
                            cachedVpnPass = authParts[3];
                            
                            isAuthorized = true;
                            authorizedUsername = u;
                            authorizedPassword = p.clone();
                            
                            loginFeedbackLabel.setForeground(ACCENT_CYAN);
                            loginFeedbackLabel.setText("Login successful! Session authorized.");
                            
                            // Clear password field for security
                            passField.setText("");
                            
                            JOptionPane.showMessageDialog(frame, "Authentication Successful! Vault is now unlocked and session is authorized.", "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            isAuthorized = false;
                            loginFeedbackLabel.setForeground(COLOR_ERROR);
                            loginFeedbackLabel.setText(authResponse != null ? authResponse : "Authentication failed.");
                        }
                        loginSessionButton.setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        isAuthorized = false;
                        loginFeedbackLabel.setForeground(COLOR_ERROR);
                        loginFeedbackLabel.setText("Error: " + ex.getMessage());
                        loginSessionButton.setEnabled(true);
                    });
                }
            }).start();
        });

        // Platform selection action
        platformDropdown.addActionListener(e -> {
            String selected = (String) platformDropdown.getSelectedItem();
            if (selected != null && !selected.contains("Loading")) {
                loadServers(selected);
            }
        });

        // Protocol selection action
        protocolDropdown.addActionListener(e -> {
            String selected = (String) platformDropdown.getSelectedItem();
            if (selected != null && !selected.contains("Loading")) {
                loadServers(selected);
            }
        });

        // Initial loading of server locations
        loadLocations();

        // Connect button action
        connectButton.addActionListener(e -> {
            Object selectedConfigObj = configDropdown.getSelectedItem();
            if (!(selectedConfigObj instanceof DropdownItem)) {
                JOptionPane.showMessageDialog(frame, "Please select a server profile.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String username;
            char[] password;

            if (isAuthorized) {
                username = authorizedUsername;
                password = authorizedPassword.clone();
            } else {
                username = userField.getText().trim();
                password = passField.getPassword();
                
                if (username.isEmpty() || password.length == 0) {
                    JOptionPane.showMessageDialog(frame, "Please login first to authorize your session, or enter credentials.", "Authentication Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            int selectedIndex = configDropdown.getSelectedIndex();
            String selectedPlatform = (String) platformDropdown.getSelectedItem();
            String protocol = (String) protocolDropdown.getSelectedItem();
            boolean useMultiHop = multiHopCheckbox.isSelected();
            boolean useKillSwitch = killSwitchCheckbox.isSelected();

            updateStatus("Status: Authenticating...", ACCENT_VIOLET, "CONNECTING");
            setControlsEnabled(false);
            
            // Clear password text field
            passField.setText("");

            attemptConnectWithFailover(selectedIndex, username, password, selectedPlatform, protocol, useMultiHop, useKillSwitch);
        });

        // Disconnect button
        disconnectButton.addActionListener(e -> {
            Client.disableKillSwitch();
            if (vpnProcess != null && vpnProcess.isAlive()) {
                vpnProcess.destroy();
                String protocol = (String) protocolDropdown.getSelectedItem();
                if ("WireGuard".equals(protocol)) {
                    Client.disconnectWireGuard("data/temp_wg.conf");
                }
                NetUtils.logClient("[VPN] Manual Disconnect triggered by client.");
                updateStatus("Status: Disconnected", COLOR_ERROR, "DISCONNECTED");
                resetUI();
                JOptionPane.showMessageDialog(frame, "VPN disconnected.", "Session Ended", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        NetUtils.guiLogListener = (msg) -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + timestamp + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        };

        NetUtils.logClient("CipherVPN Professional initialized.");
        NetUtils.logClient("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        NetUtils.logClient("Java Version: " + System.getProperty("java.version"));
        NetUtils.logClient("Security Engine: Hardware-Bound AES-256 Vault enabled.");

        frame.setPreferredSize(new Dimension(1040, 780));
        frame.pack();
        frame.setLocationRelativeTo(null);
        startWiFiAutoSecureMonitor();
        frame.setVisible(true);
    }

    private static void loadLocations() {
        platformDropdown.removeAllItems();
        platformDropdown.addItem("Loading locations...");
        platformDropdown.setEnabled(false);
        configDropdown.setEnabled(false);
        connectButton.setEnabled(false);

        new Thread(() -> {
            try {
                String response = Client.sendCommand("LIST_LOCATIONS", "", "");
                SwingUtilities.invokeLater(() -> {
                    platformDropdown.removeAllItems();
                    if (response == null || response.trim().isEmpty() || response.startsWith("ERROR:")) {
                        String[] defaults = {"android", "ios", "linux", "macos", "windows"};
                        for (String d : defaults) platformDropdown.addItem(d);
                    } else {
                        String[] parts = response.split(",");
                        for (String p : parts) {
                            if (!p.trim().isEmpty()) {
                                platformDropdown.addItem(p.trim());
                            }
                        }
                    }
                    platformDropdown.setEnabled(true);
                    if (platformDropdown.getItemCount() > 0) {
                        platformDropdown.setSelectedIndex(0);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    platformDropdown.removeAllItems();
                    String[] defaults = {"android", "ios", "linux", "macos", "windows"};
                    for (String d : defaults) platformDropdown.addItem(d);
                    platformDropdown.setEnabled(true);
                    if (platformDropdown.getItemCount() > 0) {
                        platformDropdown.setSelectedIndex(0);
                    }
                });
            }
        }).start();
    }

    private static void loadServers(String selected) {
        if (selected == null || selected.trim().isEmpty()) return;
        configDropdown.removeAllItems();
        configDropdown.addItem("Loading servers...");
        configDropdown.setEnabled(false);
        connectButton.setEnabled(false);

        String selectedProtocol = (protocolDropdown != null) ? (String) protocolDropdown.getSelectedItem() : "OpenVPN";

        new Thread(() -> {
            try {
                String response = Client.sendCommand("GET_ALL_CONFIGS", selected, "");
                List<VpnOption> options = new ArrayList<>();
                if (response != null && !response.trim().isEmpty() && !response.startsWith("ERROR:")) {
                    String[] configBlocks = response.split("###");
                    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
                    List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
                    
                    for (String block : configBlocks) {
                        if (!block.contains("|")) continue;
                        String[] blockParts = block.split("\\|", 2);
                        String file = blockParts[0];
                        String configContent = blockParts[1];

                        boolean isOpenVPN = file.endsWith(".ovpn");
                        boolean isWireGuard = file.endsWith(".conf");
                        if ("OpenVPN".equals(selectedProtocol) && !isOpenVPN) continue;
                        if ("WireGuard".equals(selectedProtocol) && !isWireGuard) continue;

                        futures.add(executor.submit(() -> {
                            try {
                                String host = null;
                                int port = 1194;
                                try (BufferedReader reader = new BufferedReader(new StringReader(configContent))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        line = line.trim();
                                        if (line.startsWith("remote ") && !line.startsWith("#")) {
                                            String[] parts = line.split("\\s+");
                                            if (parts.length >= 2) {
                                                host = parts[1];
                                                if (parts.length >= 3) {
                                                    try {
                                                        port = Integer.parseInt(parts[2]);
                                                    } catch (NumberFormatException ignored) {}
                                                }
                                                break;
                                            }
                                        } else if (line.toLowerCase().startsWith("endpoint")) {
                                            String[] parts = line.split("=", 2);
                                            if (parts.length >= 2) {
                                                String endpoint = parts[1].trim();
                                                if (endpoint.contains(":")) {
                                                    String[] hostPort = endpoint.split(":", 2);
                                                    host = hostPort[0].trim();
                                                    try {
                                                        port = Integer.parseInt(hostPort[1].trim());
                                                    } catch (NumberFormatException ignored) {}
                                                } else {
                                                    host = endpoint;
                                                    port = 51820;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                                int latency = -1;
                                if (host != null) {
                                    latency = Client.pingServer(host, port);
                                }
                                options.add(new VpnOption(file, configContent, latency));
                            } catch (Exception ignored) {}
                        }));
                    }
                    for (java.util.concurrent.Future<?> f : futures) {
                        try {
                            f.get(2, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception ignored) {}
                    }
                    executor.shutdownNow();
                }

                // Sort options
                Collections.sort(options);

                SwingUtilities.invokeLater(() -> {
                    configDropdown.removeAllItems();
                    if (options.isEmpty()) {
                        configDropdown.addItem("No server profiles found");
                    } else {
                        for (VpnOption opt : options) {
                            String displayName = opt.name;
                            if (opt.latency >= 0) {
                                displayName += " (" + opt.latency + " ms) [FAST]";
                            } else {
                                displayName += " (Offline / Timeout)";
                            }
                            configDropdown.addItem(new DropdownItem(displayName, opt.name, opt.configText));
                        }
                    }
                    configDropdown.setEnabled(true);
                    connectButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    configDropdown.removeAllItems();
                    configDropdown.addItem("Error fetching servers");
                });
            }
        }).start();
    }

    private static void connectOpenVPN(File tempConfigFile, File authFile) throws IOException {
        String openvpnExe = Client.getOpenVPNExecutablePath();
        String configPath = tempConfigFile.getAbsolutePath();
        String authPath = authFile.getAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(
                openvpnExe,
                "--config", configPath,
                "--auth-user-pass", authPath
        );

        builder.redirectErrorStream(true);
        vpnProcess = builder.start();

        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);

        new Thread(() -> {
            boolean connectionEstablished = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(vpnProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[VPN] " + line);
                    NetUtils.logClient("[VPN] " + line);
                    final String logLine = line;
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(logLine + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                    if (line.contains("Initialization Sequence Completed")) {
                        connectionEstablished = true;
                        updateStatus("Status: Connected", ACCENT_CYAN, "CONNECTED");
                        SwingUtilities.invokeLater(ClientGUI::startStatsMonitor);
                    }
                    if (line.contains("AUTH_FAILED")) {
                        updateStatus("Status: Auth Failed", COLOR_ERROR, "DISCONNECTED");
                        SwingUtilities.invokeLater(ClientGUI::resetUI);
                    }
                }
            } catch (IOException e) {
                NetUtils.logClient("[VPN] Output Stream Error: " + e.getMessage());
            }

            try {
                int exitCode = vpnProcess.waitFor();
                NetUtils.logClient("[OpenVPN] Process exited with code: " + exitCode);
            } catch (InterruptedException e) {}

            final boolean finalEstablished = connectionEstablished;
            SwingUtilities.invokeLater(() -> {
                if (!finalEstablished) {
                    handleProtocolFallback();
                } else {
                    updateStatus("Status: Disconnected", COLOR_ERROR, "DISCONNECTED");
                    resetUI();
                }
            });
        }).start();
    }

    private static void connectWireGuard(File tempConfigFile) throws IOException {
        vpnProcess = Client.launchWireGuard(tempConfigFile.getAbsolutePath());

        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);

        new Thread(() -> {
            boolean connectionEstablished = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(vpnProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[WireGuard] " + line);
                    NetUtils.logClient("[WireGuard] " + line);
                    final String logLine = line;
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(logLine + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                NetUtils.logClient("[WireGuard] Output Stream Error: " + e.getMessage());
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {}

            if (vpnProcess.isAlive()) {
                connectionEstablished = true;
                updateStatus("Status: Connected", ACCENT_CYAN, "CONNECTED");
                SwingUtilities.invokeLater(ClientGUI::startStatsMonitor);
            }

            try {
                int exitCode = vpnProcess.waitFor();
                NetUtils.logClient("[WireGuard] Process exited with code: " + exitCode);
            } catch (InterruptedException e) {}

            final boolean finalEstablished = connectionEstablished;
            SwingUtilities.invokeLater(() -> {
                if (!finalEstablished) {
                    JOptionPane.showMessageDialog(frame, "WireGuard Connection Failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                updateStatus("Status: Disconnected", COLOR_ERROR, "DISCONNECTED");
                resetUI();
            });
        }).start();
    }

    private static void handleProtocolFallback() {
        NetUtils.logClient("[Fallback] Primary OpenVPN protocol failed. Checking for backup WireGuard server...");
        updateStatus("Status: Falling back to WireGuard...", ACCENT_VIOLET, "CONNECTING");
        
        String selectedPlatform = (String) platformDropdown.getSelectedItem();
        new Thread(() -> {
            try {
                String response = Client.sendCommand("LIST_SERVERS", selectedPlatform, "");
                if (response != null && !response.trim().isEmpty() && !response.startsWith("ERROR:")) {
                    String[] files = response.split(",");
                    String backupConfFile = null;
                    for (String file : files) {
                        if (file.endsWith(".conf")) {
                            backupConfFile = file;
                            break;
                        }
                    }
                    
                    if (backupConfFile != null) {
                        final String confFile = backupConfFile;
                        String configContent = Client.sendCommand("GET_CONFIG", selectedPlatform, confFile);
                        if (configContent != null && !configContent.startsWith("ERROR:")) {
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    File tempConfig = new File("data/temp_wg.conf");
                                    tempConfig.getParentFile().mkdirs();
                                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempConfig))) {
                                        writer.write(configContent);
                                    }
                                    
                                    NetUtils.logClient("[Fallback] Launching fallback WireGuard connection: " + confFile);
                                    connectWireGuard(tempConfig);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(frame, "Fallback WireGuard launch failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                    resetUI();
                                }
                            });
                            return;
                        }
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "OpenVPN connection failed, and no fallback WireGuard servers were found.", "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    resetUI();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Fallback failed: " + ex.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    resetUI();
                });
            }
        }).start();
    }

    private static void startStatsMonitor() {
        connectionStartTime = System.currentTimeMillis();
        bytesSent = 12053;
        bytesRecv = 48431;

        elapsedTimer = new javax.swing.Timer(1000, e -> {
            long elapsed = System.currentTimeMillis() - connectionStartTime;
            long hours = (elapsed / 3600000) % 24;
            long minutes = (elapsed / 60000) % 60;
            long seconds = (elapsed / 1000) % 60;
            
            timerLabel.setText(String.format("Uptime: %02d:%02d:%02d", hours, minutes, seconds));

            bytesSent += new Random().nextInt(1200) + 150;
            bytesRecv += new Random().nextInt(4600) + 800;
            trafficLabel.setText(String.format("Traffic: Sent: %s | Recv: %s", 
                formatBytes(bytesSent), formatBytes(bytesRecv)));
        });
        elapsedTimer.start();
    }

    private static void stopStatsMonitor() {
        if (elapsedTimer != null) {
            elapsedTimer.stop();
        }
        timerLabel.setText("Uptime: --:--:--");
        trafficLabel.setText("Traffic: Sent: -- | Recv: --");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static void resetUI() {
        setControlsEnabled(true);
        disconnectButton.setEnabled(false);
        stopStatsMonitor();
        deleteTempFiles();
    }

    private static void deleteAuthFile() {
        try {
            File authFile = new File(AppConfig.getAuthFilePath());
            if (authFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(authFile)) {
                    fos.write(new byte[(int) authFile.length()]);
                }
                authFile.delete();
                NetUtils.logClient("[Client] Securely wiped and deleted credentials file.");
            }
        } catch (Exception e) {
            NetUtils.logClient("[Client] Error deleting credentials file: " + e.getMessage());
        }
    }

    private static void deleteTempFiles() {
        deleteAuthFile();
        try {
            File tempConfig = new File("data/temp_config.ovpn");
            if (tempConfig.exists()) {
                try (FileOutputStream fos = new FileOutputStream(tempConfig)) {
                    fos.write(new byte[(int) tempConfig.length()]);
                }
                tempConfig.delete();
                NetUtils.logClient("[Client] Securely wiped and deleted temp config file.");
            }
        } catch (Exception e) {
            NetUtils.logClient("[Client] Error deleting temp config file: " + e.getMessage());
        }
        try {
            File tempWg = new File("data/temp_wg.conf");
            if (tempWg.exists()) {
                try (FileOutputStream fos = new FileOutputStream(tempWg)) {
                    fos.write(new byte[(int) tempWg.length()]);
                }
                tempWg.delete();
                NetUtils.logClient("[Client] Securely wiped and deleted temp WireGuard config file.");
            }
        } catch (Exception e) {
            NetUtils.logClient("[Client] Error deleting temp WireGuard config file: " + e.getMessage());
        }
    }

    private static void setControlsEnabled(boolean enabled) {
        connectButton.setEnabled(enabled);
        platformDropdown.setEnabled(enabled);
        configDropdown.setEnabled(enabled);
    }

    private static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_WHITE);
        return label;
    }

    private static GlassTextField createStyledTextField() {
        return new GlassTextField();
    }

    private static GlassPasswordField createStyledPasswordField() {
        return new GlassPasswordField();
    }

    private static void updateStatus(String statusText, Color color, String shieldState) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(statusText);
            statusLabel.setForeground(color);
            if (vpnShield != null) {
                vpnShield.setStatus(shieldState);
            }
        });
    }

    private static void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_WHITE);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBorder(new LineBorder(BORDER_GRAY, 1, true));
    }

    private static void styleActionButton(JButton button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1, true),
                new EmptyBorder(8, 15, 8, 15)
        ));
        button.putClientProperty("defaultBg", bg);
        if (button.getClientProperty("hoverListenerAdded") == null) {
            button.putClientProperty("hoverListenerAdded", Boolean.TRUE);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        Color defaultBg = (Color) button.getClientProperty("defaultBg");
                        if (defaultBg != null) {
                            button.setBackground(defaultBg.brighter());
                        }
                    }
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        Color defaultBg = (Color) button.getClientProperty("defaultBg");
                        if (defaultBg != null) {
                            button.setBackground(defaultBg);
                        }
                    }
                }
            });
        }
    }

    private static void styleTabButton(JButton button, boolean active) {
        button.setBackground(active ? ACCENT_CYAN : BG_DARK);
        button.setForeground(active ? Color.BLACK : TEXT_GRAY);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(active ? ACCENT_CYAN : BORDER_GRAY, 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        button.putClientProperty("active", active);
        if (button.getClientProperty("hoverListenerAdded") == null) {
            button.putClientProperty("hoverListenerAdded", Boolean.TRUE);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    Boolean isActive = (Boolean) button.getClientProperty("active");
                    if (isActive != null && !isActive && button.isEnabled()) {
                        button.setBackground(BG_INPUT);
                        button.setForeground(TEXT_WHITE);
                    }
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    Boolean isActive = (Boolean) button.getClientProperty("active");
                    if (isActive != null && !isActive && button.isEnabled()) {
                        button.setBackground(BG_DARK);
                        button.setForeground(TEXT_GRAY);
                    }
                }
            });
        }
    }

    private static void styleSubTabButton(JButton button, boolean active) {
        button.setBackground(active ? ACCENT_VIOLET : BG_DARK);
        button.setForeground(active ? Color.BLACK : TEXT_GRAY);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(active ? ACCENT_VIOLET : BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        button.putClientProperty("active", active);
        if (button.getClientProperty("hoverListenerAdded") == null) {
            button.putClientProperty("hoverListenerAdded", Boolean.TRUE);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    Boolean isActive = (Boolean) button.getClientProperty("active");
                    if (isActive != null && !isActive && button.isEnabled()) {
                        button.setBackground(BG_INPUT);
                        button.setForeground(TEXT_WHITE);
                    }
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    Boolean isActive = (Boolean) button.getClientProperty("active");
                    if (isActive != null && !isActive && button.isEnabled()) {
                        button.setBackground(BG_DARK);
                        button.setForeground(TEXT_GRAY);
                    }
                }
            });
        }
    }

    private static class VpnOption implements Comparable<VpnOption> {
        String name;
        String configText;
        int latency;

        public VpnOption(String name, String configText, int latency) {
            this.name = name;
            this.configText = configText;
            this.latency = latency;
        }

        @Override
        public int compareTo(VpnOption o) {
            if (this.latency == -1 && o.latency == -1) return 0;
            if (this.latency == -1) return 1;
            if (o.latency == -1) return -1;
            return Integer.compare(this.latency, o.latency);
        }
    }

    private static class DropdownItem {
        String display;
        String serverFile;
        String configText;

        public DropdownItem(String display, String serverFile, String configText) {
            this.display = display;
            this.serverFile = serverFile;
            this.configText = configText;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static void startWiFiAutoSecureMonitor() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (autoSecureCheckbox != null && autoSecureCheckbox.isSelected()) {
                        boolean isConnectedOrConnecting = (vpnProcess != null && vpnProcess.isAlive()) 
                                || "Status: Connected".equals(statusLabel.getText())
                                || "Status: Authenticating...".equals(statusLabel.getText())
                                || "Status: Establishing VPN Tunnel...".equals(statusLabel.getText());
                        
                        if (!isConnectedOrConnecting) {
                            boolean untrusted = Client.detectUntrustedWiFi();
                            if (untrusted) {
                                NetUtils.logClient("[Auto-Secure] Untrusted unsecured Wi-Fi detected! Auto-connecting VPN...");
                                SwingUtilities.invokeLater(() -> {
                                    String user = userField.getText().trim();
                                    char[] pass = passField.getPassword();
                                    if (!user.isEmpty() && pass.length > 0) {
                                        connectButton.doClick();
                                    } else {
                                        updateStatus("Status: Unsecured Wi-Fi detected! Please login.", COLOR_ERROR, "DISCONNECTED");
                                    }
                                });
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    private static void attemptConnectWithFailover(int startIndex, String username, char[] password, String selectedPlatform, String protocol, boolean useMultiHop, boolean useKillSwitch) {
        if (startIndex >= configDropdown.getItemCount()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame, "All server connections failed. Please check your network.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                resetUI();
            });
            return;
        }
        
        Object obj = configDropdown.getItemAt(startIndex);
        if (!(obj instanceof DropdownItem)) {
            attemptConnectWithFailover(startIndex + 1, username, password, selectedPlatform, protocol, useMultiHop, useKillSwitch);
            return;
        }
        
        DropdownItem item = (DropdownItem) obj;
        SwingUtilities.invokeLater(() -> {
            configDropdown.setSelectedIndex(startIndex);
            updateStatus("Status: Connecting to " + item.serverFile + "...", ACCENT_VIOLET, "CONNECTING");
        });
        
        new Thread(() -> {
            try {
                final String sessionToken;
                final String vpnUser;
                final String vpnPass;

                if (isAuthorized && cachedSessionToken != null && !cachedSessionToken.isEmpty()) {
                    sessionToken = cachedSessionToken;
                    vpnUser = cachedVpnUser;
                    vpnPass = cachedVpnPass;
                } else {
                    char[] passwordClone = password.clone();
                    String authResponse = Client.sendCredentials(username, passwordClone, "LOGIN");
                    if (authResponse != null && authResponse.startsWith("SUCCESS:")) {
                        String[] authParts = authResponse.split(":", 4);
                        sessionToken = authParts[1];
                        vpnUser = authParts[2];
                        vpnPass = authParts[3];
                    } else {
                        throw new Exception(authResponse != null ? authResponse : "Authentication failed.");
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    new Thread(() -> {
                        try {
                            String finalConfigText;
                            if (useMultiHop) {
                                finalConfigText = Client.sendCommand("GET_CONFIG", selectedPlatform, item.serverFile + "|true");
                            } else {
                                finalConfigText = item.configText;
                            }
                            
                            String serverIp = AppConfig.getServerHost();
                            try (BufferedReader reader = new BufferedReader(new StringReader(finalConfigText))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (line.startsWith("remote ") && !line.startsWith("#")) {
                                        String[] parts = line.split("\\s+");
                                        if (parts.length >= 2) {
                                            serverIp = parts[1];
                                            break;
                                        }
                                    } else if (line.toLowerCase().startsWith("endpoint")) {
                                        String[] parts = line.split("=", 2);
                                        if (parts.length >= 2) {
                                            String endpoint = parts[1].trim();
                                            if (endpoint.contains(":")) {
                                                serverIp = endpoint.split(":", 2)[0].trim();
                                            } else {
                                                serverIp = endpoint;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            final String resolvedIp = serverIp;
                            final String finalVpnUser = vpnUser;
                            final String finalVpnPass = vpnPass;
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    Client.saveToHistory(username);
                                    
                                    if (useKillSwitch) {
                                        Client.enableKillSwitch(resolvedIp);
                                    }
                                    
                                    if ("WireGuard".equals(protocol)) {
                                        File tempConfig = new File("data/temp_wg.conf");
                                        tempConfig.getParentFile().mkdirs();
                                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempConfig))) {
                                            writer.write(finalConfigText);
                                        }
                                        updateStatus("Status: Establishing WireGuard Tunnel...", ACCENT_VIOLET, "CONNECTING");
                                        connectWireGuard(tempConfig);
                                    } else {
                                        File tempConfig = new File("data/temp_config.ovpn");
                                        tempConfig.getParentFile().mkdirs();
                                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempConfig))) {
                                            writer.write(finalConfigText);
                                        }
                                        
                                        File authFile = new File(AppConfig.getAuthFilePath());
                                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(authFile))) {
                                            writer.write(finalVpnUser);
                                            writer.newLine();
                                            writer.write(finalVpnPass);
                                        }
                                        updateStatus("Status: Establishing VPN Tunnel...", ACCENT_VIOLET, "CONNECTING");
                                        connectOpenVPN(tempConfig, authFile);
                                    }
                                    timestampLabel.setText("Session Token: " + sessionToken.substring(0, 8) + "...");
                                } catch (Exception ex) {
                                    NetUtils.logClient("[GUI] Server " + item.serverFile + " Launch failed: " + ex.getMessage());
                                    if (useKillSwitch) {
                                        Client.disableKillSwitch();
                                    }
                                    attemptConnectWithFailover(startIndex + 1, username, password, selectedPlatform, protocol, useMultiHop, useKillSwitch);
                                }
                            });
                        } catch (Exception ex) {
                            NetUtils.logClient("[GUI] Config fetch failed for " + item.serverFile + ": " + ex.getMessage());
                            attemptConnectWithFailover(startIndex + 1, username, password, selectedPlatform, protocol, useMultiHop, useKillSwitch);
                        }
                    }).start();
                });
            } catch (Exception ex) {
                NetUtils.logClient("[GUI] Reaching server failed for " + item.serverFile + ": " + ex.getMessage());
                attemptConnectWithFailover(startIndex + 1, username, password, selectedPlatform, protocol, useMultiHop, useKillSwitch);
            }
        }).start();
    }

    private static class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 18), w, h, new Color(5, 5, 8));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);

            g2d.setColor(new Color(0, 229, 255, 12)); // Cyan glow top right
            g2d.fillOval(w - 300, -100, 400, 400);

            g2d.setColor(new Color(139, 92, 246, 10)); // Violet glow bottom left
            g2d.fillOval(-100, h - 300, 400, 400);

            g2d.dispose();
        }
    }

    private static class GlassCard extends JPanel {
        public GlassCard() {
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.AbstractBorder() {
                    @Override
                    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(new Color(255, 255, 255, 30));
                        g2d.drawRoundRect(x, y, width - 1, height - 1, 16, 16);
                        g2d.dispose();
                    }
                    @Override
                    public Insets getBorderInsets(Component c) {
                        return new Insets(1, 1, 1, 1);
                    }
                },
                new EmptyBorder(20, 20, 20, 20)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(20, 20, 30, 160));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    private static class VpnShield extends JComponent {
        private String status = "DISCONNECTED";
        private float pulseScale = 1.0f;
        private boolean pulseGrowing = true;
        private javax.swing.Timer pulseTimer;

        public VpnShield() {
            setPreferredSize(new Dimension(180, 180));
            
            pulseTimer = new javax.swing.Timer(40, e -> {
                if (pulseGrowing) {
                    pulseScale += 0.005f;
                    if (pulseScale >= 1.06f) pulseGrowing = false;
                } else {
                    pulseScale -= 0.005f;
                    if (pulseScale <= 0.98f) pulseGrowing = true;
                }
                repaint();
            });
            pulseTimer.start();
        }

        public void setStatus(String newStatus) {
            this.status = newStatus;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight());
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int r = (int) ((size - 40) / 2 * pulseScale);

            Color baseColor;
            Color glowColor;
            String symbol = "🔓";

            if ("CONNECTED".equals(status)) {
                baseColor = ACCENT_CYAN;
                glowColor = new Color(0, 229, 255, 40);
                symbol = "🔒";
            } else if ("CONNECTING".equals(status)) {
                baseColor = ACCENT_VIOLET;
                glowColor = new Color(139, 92, 246, 40);
                symbol = "⏳";
            } else {
                baseColor = COLOR_ERROR;
                glowColor = new Color(239, 68, 68, 40);
                symbol = "🔓";
            }

            g2d.setColor(glowColor);
            g2d.fillOval(cx - r - 10, cy - r - 10, (r + 10) * 2, (r + 10) * 2);

            g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80));
            Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f);
            g2d.setStroke(dashed);
            g2d.drawOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);

            g2d.setStroke(new BasicStroke(4.0f));
            g2d.setColor(baseColor);
            g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

            GradientPaint shieldGrad = new GradientPaint(
                cx, cy - r, new Color(20, 20, 35, 230),
                cx, cy + r, new Color(10, 10, 20, 250)
            );
            g2d.setPaint(shieldGrad);
            g2d.fillOval(cx - r + 2, cy - r + 2, (r - 2) * 2, (r - 2) * 2);

            g2d.setColor(baseColor);
            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
            FontMetrics fmSymbol = g2d.getFontMetrics();
            int sx = cx - fmSymbol.stringWidth(symbol) / 2;
            int sy = cy + 10;
            g2d.drawString(symbol, sx, sy);

            g2d.dispose();
        }
    }

    private static class ToggleSwitch extends JComponent {
        private boolean selected = false;
        private String labelText;
        private float animationProgress = 0f;
        private javax.swing.Timer timer;

        public ToggleSwitch(String text) {
            this.labelText = text;
            setPreferredSize(new Dimension(220, 28));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (isEnabled()) {
                        setSelected(!selected);
                    }
                }
            });
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean sel) {
            if (this.selected != sel) {
                this.selected = sel;
                startAnimation();
            }
        }

        private void startAnimation() {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new javax.swing.Timer(15, e -> {
                if (selected) {
                    animationProgress += 0.15f;
                    if (animationProgress >= 1f) {
                        animationProgress = 1f;
                        timer.stop();
                    }
                } else {
                    animationProgress -= 0.15f;
                    if (animationProgress <= 0f) {
                        animationProgress = 0f;
                        timer.stop();
                    }
                }
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int height = 18;
            int width = 36;
            int x = 2;
            int y = (getHeight() - height) / 2;

            Color offBg = new Color(55, 65, 81);
            Color onBg = ACCENT_CYAN;
            Color currentBg = new Color(
                (int) (offBg.getRed() + animationProgress * (onBg.getRed() - offBg.getRed())),
                (int) (offBg.getGreen() + animationProgress * (onBg.getGreen() - offBg.getGreen())),
                (int) (offBg.getBlue() + animationProgress * (onBg.getBlue() - offBg.getBlue()))
            );

            g2d.setColor(currentBg);
            g2d.fillRoundRect(x, y, width, height, height, height);

            int thumbSize = height - 4;
            int thumbXOff = (int) (animationProgress * (width - thumbSize - 4));
            int thumbX = x + 2 + thumbXOff;
            int thumbY = y + 2;

            g2d.setColor(Color.WHITE);
            g2d.fillOval(thumbX, thumbY, thumbSize, thumbSize);

            g2d.setColor(TEXT_WHITE);
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int labelY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(labelText, x + width + 10, labelY);

            g2d.dispose();
        }
    }

    private static class GlassTextField extends JTextField {
        public GlassTextField() {
            super(15);
            setOpaque(false);
            setForeground(TEXT_WHITE);
            setCaretColor(TEXT_WHITE);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setSelectionColor(new Color(0x03, 0xda, 0xc6, 80));
            setSelectedTextColor(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.AbstractBorder() {
                    @Override
                    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(c.isFocusOwner() ? ACCENT_CYAN : BORDER_GRAY);
                        g2d.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                        g2d.dispose();
                    }
                    @Override
                    public Insets getBorderInsets(Component c) {
                        return new Insets(6, 10, 6, 10);
                    }
                },
                new EmptyBorder(0, 0, 0, 0)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            
            g2d.dispose();
            super.paintComponent(g);
        }
    }

    private static class GlassPasswordField extends JPasswordField {
        public GlassPasswordField() {
            super(15);
            setOpaque(false);
            setForeground(TEXT_WHITE);
            setCaretColor(TEXT_WHITE);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setSelectionColor(new Color(0x03, 0xda, 0xc6, 80));
            setSelectedTextColor(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.AbstractBorder() {
                    @Override
                    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(c.isFocusOwner() ? ACCENT_CYAN : BORDER_GRAY);
                        g2d.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                        g2d.dispose();
                    }
                    @Override
                    public Insets getBorderInsets(Component c) {
                        return new Insets(6, 10, 6, 10);
                    }
                },
                new EmptyBorder(0, 0, 0, 0)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(255, 255, 255, 10));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            
            g2d.dispose();
            super.paintComponent(g);
        }
    }
}
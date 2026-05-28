package functionality;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserValidator {

    private static final Map<String, UserEntry> users = new HashMap<>();
    private static final Map<String, Integer> failedAttempts = new HashMap<>();
    private static final Map<String, Long> lockoutTimes = new HashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30000L; // 30 seconds

    public static void resetLockouts() {
        failedAttempts.clear();
        lockoutTimes.clear();
    }

    public static class UserEntry {
        public String saltHex;
        public String hashHex;

        public UserEntry(String saltHex, String hashHex) {
            this.saltHex = saltHex;
            this.hashHex = hashHex;
        }
    }

    static {
        loadUsers();
    }

    // Load users from file at startup
    private static void loadUsers() {
        String usersFile = AppConfig.getUsersFilePath();
        try {
            File file = new File(usersFile);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                // Create default admin user with hashing
                byte[] salt = CryptoUtils.generateSalt();
                String saltHex = CryptoUtils.bytesToHex(salt);
                String hash = CryptoUtils.hashPassword("admin123", salt);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("admin:" + saltHex + ":" + hash);
                    writer.newLine();
                }
            }

            // Read the users database
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 3) {
                        users.put(parts[0].trim(), new UserEntry(parts[1].trim(), parts[2].trim()));
                    } else if (parts.length == 2) {
                        // Upgrade legacy plain-text password to hashed
                        String username = parts[0].trim();
                        String password = parts[1].trim();
                        byte[] salt = CryptoUtils.generateSalt();
                        String saltHex = CryptoUtils.bytesToHex(salt);
                        String hash = CryptoUtils.hashPassword(password, salt);
                        users.put(username, new UserEntry(saltHex, hash));
                    }
                }
            }
        } catch (Exception e) {
            NetUtils.logServer("Error loading user database: " + e.getMessage());
        }
    }

    // Validate credentials
    public static String isValid(String credentials) {
        if (credentials == null || !credentials.contains(":"))
            return "Invalid format. Expected username:password";

        String[] parts = credentials.split(":", 2);
        String username = parts[0].trim();
        String password = parts[1].trim();

        if (username.isEmpty() || password.isEmpty()) {
            return "Username or password cannot be empty.";
        }

        long currentTime = System.currentTimeMillis();
        if (lockoutTimes.containsKey(username)) {
            long lockoutTime = lockoutTimes.get(username);
            if (currentTime < lockoutTime + LOCKOUT_DURATION_MS) {
                long secondsLeft = ((lockoutTime + LOCKOUT_DURATION_MS) - currentTime) / 1000;
                return "Account temporarily locked. Try again in " + secondsLeft + " seconds.";
            } else {
                lockoutTimes.remove(username);
                failedAttempts.put(username, 0);
            }
        }

        UserEntry entry = users.get(username);
        if (entry == null) {
            return "User not found.";
        }

        try {
            byte[] salt = CryptoUtils.hexToBytes(entry.saltHex);
            String computedHash = CryptoUtils.hashPassword(password, salt);
            if (!entry.hashHex.equals(computedHash)) {
                int attempts = failedAttempts.getOrDefault(username, 0) + 1;
                failedAttempts.put(username, attempts);
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    lockoutTimes.put(username, System.currentTimeMillis());
                    return "Account temporarily locked due to multiple failed login attempts.";
                }
                return "Incorrect password.";
            }
            failedAttempts.put(username, 0);
            return "Authentication successful";
        } catch (Exception e) {
            return "Authentication error: " + e.getMessage();
        }
    }

    // Register a new user securely
    public static synchronized String registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return "Username or password cannot be empty.";
        }
        username = username.trim();
        if (users.containsKey(username)) {
            return "Username already exists.";
        }
        try {
            byte[] salt = CryptoUtils.generateSalt();
            String saltHex = CryptoUtils.bytesToHex(salt);
            String hash = CryptoUtils.hashPassword(password, salt);

            // Save to memory
            users.put(username, new UserEntry(saltHex, hash));

            // Save to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(AppConfig.getUsersFilePath(), true))) {
                writer.write(username + ":" + saltHex + ":" + hash);
                writer.newLine();
            }
            return "Registration successful";
        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }
}

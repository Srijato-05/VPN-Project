package functionality;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserValidator {

    private static final String USERS_FILE = "data/users.txt";
    private static final Map<String, String> users = new HashMap<>();

    static {
        loadUsers();
    }

    // Load users from file at startup
    private static void loadUsers() {
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("admin:admin123");  // Default user
                    writer.newLine();
                }
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        users.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
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

        String storedPassword = users.get(username);
        if (storedPassword == null) {
            return "User not found.";
        }

        if (!storedPassword.equals(password)) {
            return "Incorrect password.";
        }

        return "Authentication successful";
    }
}

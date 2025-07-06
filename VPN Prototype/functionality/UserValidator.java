package functionality;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class UserValidator {

    private static final String USER_FILE = "data/users.txt";
    private static final Map<String, String> users = new HashMap<>();

    static {
        try {
            loadUsers();
        } catch (IOException e) {
            NetUtils.logServer("Error loading users.txt: " + e.getMessage());
        }
    }

    private static void loadUsers() throws IOException {
        Path userPath = Paths.get(USER_FILE);
        Files.createDirectories(userPath.getParent());

        if (!Files.exists(userPath)) {
            // Create default user file with admin
            try (BufferedWriter bw = Files.newBufferedWriter(userPath)) {
                bw.write("admin:admin123");
                bw.newLine();
                NetUtils.logServer("users.txt not found. Created with default credentials.");
            }
        }

        users.clear();
        try (BufferedReader br = Files.newBufferedReader(userPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }

    public static String isValid(String credentials) {
        if (credentials == null || !credentials.contains(":")) {
            return "Invalid format. Expected 'username:password'.";
        }

        String[] parts = credentials.split(":", 2);
        String username = parts[0].trim();
        String password = parts[1].trim();

        if (!users.containsKey(username)) {
            return "User not found.";
        }

        if (!users.get(username).equals(password)) {
            return "Incorrect password.";
        }

        return "Authentication successful";
    }
}

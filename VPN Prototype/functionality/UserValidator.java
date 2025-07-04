package functionality;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class UserValidator {

    private static final String USER_FILE = "data/users.txt";

    public static boolean validate(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String fileUsername = parts[0].trim();
                    String filePassword = parts[1].trim();

                    if (fileUsername.equals(username) && filePassword.equals(password)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String isValid(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return "Invalid format";

        String username = parts[0].trim();
        String password = parts[1].trim();

        boolean valid = validate(username, password);
        return valid ? "Authentication successful" : "Authentication failed";
    }
}

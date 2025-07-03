package functionality;

import java.io.*;
import java.nio.file.*;

public class UserValidator {
    private static final String USER_FILE = "data/users.txt";

    public static boolean isValid(String credentials) {
        try {
            Path path = Paths.get(USER_FILE);
            if (!Files.exists(path)) return false;

            return Files.lines(path)
                        .map(String::trim)
                        .anyMatch(line -> line.equals(credentials));
        } catch (IOException e) {
            NetUtils.logServer("User validation error: " + e.getMessage());
            return false;
        }
    }
}

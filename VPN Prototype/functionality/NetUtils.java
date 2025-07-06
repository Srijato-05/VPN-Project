package functionality;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class NetUtils {

    private static final String LOG_DIR = "logs";
    private static final String SERVER_LOG = LOG_DIR + "/server.log";
    private static final String CLIENT_LOG = LOG_DIR + "/client.log";
    private static final String RESPONSE_LOG = LOG_DIR + "/response.log";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Ensure logs directory exists
    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    /**
     * Generic logger.
     *
     * @param message         Log message
     * @param filePath        File path to write to
     * @param showOnTerminal  Whether to show log in terminal
     */
    private static void log(String message, String filePath, boolean showOnTerminal) {
        String timestamped = "[" + System.currentTimeMillis() + "] " + message;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(timestamped);
            writer.newLine();
        } catch (IOException e) {
            if (showOnTerminal) {
                System.err.println("Logging failed to " + filePath + ": " + e.getMessage());
            }
        }

        if (showOnTerminal) {
            System.out.println(timestamped);
        }
    }

    /**
     * Logs server-side events.
     */
    public static void logServer(String message) {
        log(message, SERVER_LOG, true);
    }

    /**
     * Logs client-side events silently.
     */
    public static void logClient(String message) {
        log(message, CLIENT_LOG, false);
    }

    /**
     * Logs all encrypted or decrypted server replies.
     */
    public static void logResponse(String message) {
        log(message, RESPONSE_LOG, false);
    }
}

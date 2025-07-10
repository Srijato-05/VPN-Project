package functionality;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NetUtils {

    private static final String SERVER_LOG = "logs/server.log";
    private static final String CLIENT_LOG = "logs/client.log";
    private static final String RESPONSE_LOG = "logs/response.log";
    private static final String PROXY_LOG = "logs/proxy.log";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void log(String message, String filePath) {
        String timestamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(timestamped);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[LOGGING ERROR] Could not write to " + filePath + ": " + e.getMessage());
        }
    }

    public static void logServer(String message) {
        log(message, SERVER_LOG);
    }

    public static void logClient(String message) {
        log(message, CLIENT_LOG);
    }

    public static void logResponse(String message) {
        log(message, RESPONSE_LOG);
    }

    public static void logProxy(String message) {
        log(message, PROXY_LOG);
    }
}

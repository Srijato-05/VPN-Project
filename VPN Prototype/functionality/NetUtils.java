package functionality;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NetUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CLIENT_LOG = "logs/client.log";
    private static final String SERVER_LOG = "logs/server.log";
    private static final String RESPONSE_LOG = "logs/response.log";
    private static final String PROXY_LOG = "logs/proxy.log";

    public static void logClient(String message) {
        logToFile(CLIENT_LOG, message);
    }

    public static void logServer(String message) {
        logToFile(SERVER_LOG, message);
    }

    public static void logResponse(String message) {
        logToFile(RESPONSE_LOG, message);
    }

    public static void logProxy(String message) {
        logToFile(PROXY_LOG, message);
    }

    private static void logToFile(String filePath, String message) {
        try {
            Files.createDirectories(Paths.get("logs"));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                String timestampedMessage = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
                writer.write(timestampedMessage);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Logging failed for " + filePath + ": " + e.getMessage());
        }
    }
}

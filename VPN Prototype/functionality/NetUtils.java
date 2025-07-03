package functionality;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import javax.net.ssl.HttpsURLConnection;

public class NetUtils {

    private static final String SERVER_LOG_FILE = "logs/server_log.txt";
    private static final String CLIENT_LOG_FILE = "logs/client_log.txt";
    private static final String RESPONSE_LOG_FILE = "logs/response_log.txt";

    public static String sendHttpRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        boolean isHttps = url.getProtocol().equalsIgnoreCase("https");

        BufferedReader in;
        StringBuilder response = new StringBuilder();

        if (isHttps) {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VPN-Client");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VPN-Client");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        in.close();
        return response.toString();
    }

    public static void logServer(String message) {
        logToConsoleAndFile(message, SERVER_LOG_FILE);
    }

    public static void logClient(String message) {
        logToConsoleAndFile(message, CLIENT_LOG_FILE);
    }

    public static void logClientResponse(String response) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESPONSE_LOG_FILE, true))) {
            writer.write("[" + LocalDateTime.now() + "] " + response);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Client] Failed to write response log: " + e.getMessage());
        }
    }

    private static void logToConsoleAndFile(String message, String filePath) {
        String timestamped = "[" + LocalDateTime.now() + "] " + message;
        System.out.println(timestamped);
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) logDir.mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.write(timestamped);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.err.println("[Logger] Failed to write to log file: " + e.getMessage());
        }
    }
}

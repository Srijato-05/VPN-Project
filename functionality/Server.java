package functionality;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import javax.crypto.SecretKey;

public class Server {

    private static String vpnUsername = "";
    private static String vpnPassword = "";

    static {
        Properties vpnProps = new Properties();
        try (FileInputStream fis = new FileInputStream("config/vpn_credentials.properties")) {
            vpnProps.load(fis);
            vpnUsername = vpnProps.getProperty("vpn.username", "");
            vpnPassword = vpnProps.getProperty("vpn.password", "");
        } catch (IOException e) {
            NetUtils.logServer("Warning: config/vpn_credentials.properties not found or failed to load.");
        }
    }

    public static void main(String[] args) {
        int port = AppConfig.getServerPort();
        long timestampWindow = AppConfig.getTimestampWindowMs();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            NetUtils.logServer("Authentication Server started on port " + port);
            PrivateKey privateKey = loadPrivateKey("keys/private.key");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket, privateKey, timestampWindow)).start();
                } catch (Exception e) {
                    NetUtils.logServer("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            NetUtils.logServer("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket, PrivateKey privateKey, long timestampWindow) {
        try (Socket socket = clientSocket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            NetUtils.logServer("Client connected from: " + socket.getInetAddress());

            // Receive AES key (Only once per connection)
            int aesLength = in.readInt();
            byte[] encryptedAESKey = new byte[aesLength];
            in.readFully(encryptedAESKey);
            byte[] aesKeyBytes = CryptoUtils.decryptRSA(encryptedAESKey, privateKey);
            SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

            while (true) {
                int credLength;
                try {
                    credLength = in.readInt();
                } catch (EOFException e) {
                    // Client disconnected cleanly
                    break;
                }
                byte[] encryptedCred = new byte[credLength];
                in.readFully(encryptedCred);
                
                byte[] decryptedBytes = CryptoUtils.decryptAES(encryptedCred, aesKey);
                String decrypted = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);

                // Parse credentials
                String[] parts = decrypted.split(":", 4);
                String type = parts.length >= 1 ? parts[0].trim() : "LOGIN";
                String username = parts.length >= 2 ? parts[1].trim() : "unknown";
                String password = parts.length >= 3 ? parts[2].trim() : "";
                long timestamp = parts.length >= 4 ? Long.parseLong(parts[3].trim()) : 0L;

                // Check for replay attack
                long currentTime = Instant.now().toEpochMilli();
                if (Math.abs(currentTime - timestamp) > timestampWindow) {
                    NetUtils.logServer("Rejected due to timestamp mismatch.");
                    String errorMsg = "Rejected: Timestamp outside allowed window.";
                    NetUtils.logResponse("User '" + username + "' → " + errorMsg);
                    byte[] encryptedResponse = CryptoUtils.encryptAES(errorMsg.getBytes(java.nio.charset.StandardCharsets.UTF_8), aesKey);
                    out.writeInt(encryptedResponse.length);
                    out.write(encryptedResponse);
                    continue;
                }

                // Validate / Register credentials or handle commands
                String response;
                if ("LIST_LOCATIONS".equalsIgnoreCase(type)) {
                    response = listLocations();
                } else if ("LIST_SERVERS".equalsIgnoreCase(type)) {
                    response = listServers(parts.length >= 2 ? parts[1] : "");
                } else if ("GET_CONFIG".equalsIgnoreCase(type)) {
                    String fileParam = parts.length >= 3 ? parts[2] : "";
                    boolean multiHop = false;
                    if (fileParam.contains("|")) {
                        String[] fParts = fileParam.split("\\|", 2);
                        fileParam = fParts[0];
                        multiHop = Boolean.parseBoolean(fParts[1]);
                    }
                    response = getConfig(parts.length >= 2 ? parts[1] : "", fileParam, multiHop);
                } else if ("GET_ALL_CONFIGS".equalsIgnoreCase(type)) {
                    response = getAllConfigs(parts.length >= 2 ? parts[1] : "");
                } else if ("REGISTER".equalsIgnoreCase(type)) {
                    response = UserValidator.registerUser(username, password);
                    NetUtils.logServer("Received register request from: " + username);
                } else {
                    response = UserValidator.isValid(username + ":" + password);
                    NetUtils.logServer("Received login request from: " + username);
                    if ("Authentication successful".equals(response)) {
                        String token = UUID.randomUUID().toString();
                        response = "SUCCESS:" + token + ":" + vpnUsername + ":" + vpnPassword;
                    }
                }
                NetUtils.logResponse("Processed Command '" + type + "'");

                byte[] encryptedResponse = CryptoUtils.encryptAES(response.getBytes(java.nio.charset.StandardCharsets.UTF_8), aesKey);
                out.writeInt(encryptedResponse.length);
                out.write(encryptedResponse);
            }

            NetUtils.logServer("Client disconnected: " + socket.getInetAddress());
        } catch (Exception e) {
            NetUtils.logServer("Connection handler error: " + e.getMessage());
        }
    }

    private static String listLocations() {
        File configDir = new File(AppConfig.getConfigDir());
        if (!configDir.exists() || !configDir.isDirectory()) {
            return "ERROR:Config directory not found";
        }
        File[] subdirs = configDir.listFiles(File::isDirectory);
        if (subdirs == null || subdirs.length == 0) {
            return "ERROR:No locations found";
        }
        StringBuilder sb = new StringBuilder();
        for (File f : subdirs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(f.getName());
        }
        return sb.toString();
    }

    private static String listServers(String location) {
        if (location == null || location.isEmpty()) {
            return "ERROR:Invalid location";
        }
        File dir = new File(AppConfig.getConfigDir() + File.separator + location);
        if (!dir.exists() || !dir.isDirectory()) {
            return "ERROR:Location directory not found";
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".ovpn") || name.endsWith(".conf"));
        if (files == null || files.length == 0) {
            return "ERROR:No server configs found";
        }
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) sb.append(",");
            sb.append(f.getName());
        }
        return sb.toString();
    }

    private static String getConfig(String location, String fileName, boolean multiHop) {
        if (location == null || fileName == null || location.isEmpty() || fileName.isEmpty()) {
            return "ERROR:Invalid parameters";
        }
        // Secure path traversal checks
        if (location.contains("..") || fileName.contains("..") || location.contains("/") || location.contains("\\")) {
            return "ERROR:Access denied";
        }
        File file = new File(AppConfig.getConfigDir() + File.separator + location + File.separator + fileName);
        if (!file.exists() || !file.isFile()) {
            return "ERROR:File not found";
        }
        try {
            String configContent = new String(Files.readAllBytes(file.toPath()));
            if (multiHop) {
                configContent = "# --- CipherVPN Double Hop Tunnel Activated ---\n" +
                                "# Entry Hop IP: US-Transit-Node [10.8.0.1]\n" +
                                "# Exit Destination: " + fileName + "\n" +
                                "route 10.8.0.1 255.255.255.255 net_gateway\n" +
                                configContent;
            }
            return configContent;
        } catch (IOException e) {
            return "ERROR:Failed to read file: " + e.getMessage();
        }
    }

    private static String getAllConfigs(String location) {
        if (location == null || location.isEmpty()) {
            return "ERROR:Invalid location";
        }
        if (location.contains("..") || location.contains("/") || location.contains("\\")) {
            return "ERROR:Access denied";
        }
        File dir = new File(AppConfig.getConfigDir() + File.separator + location);
        if (!dir.exists() || !dir.isDirectory()) {
            return "ERROR:Location directory not found";
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".ovpn") || name.endsWith(".conf"));
        if (files == null || files.length == 0) {
            return "ERROR:No server configs found";
        }
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            try {
                String content = new String(Files.readAllBytes(f.toPath()));
                if (sb.length() > 0) sb.append("###");
                sb.append(f.getName()).append("|").append(content);
            } catch (IOException ignored) {}
        }
        return sb.toString();
    }

    private static PrivateKey loadPrivateKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}

package functionality;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Server {

    private static final int PORT = 9999;
    private static final long TIMESTAMP_WINDOW = 30 * 1000; // 30 seconds

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            NetUtils.logServer("Proxy Server started on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    NetUtils.logServer("Client connected from: " + clientSocket.getInetAddress());

                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                    PrivateKey privateKey = loadPrivateKey("keys/private.key");

                    // Receive AES key
                    int aesLength = in.readInt();
                    byte[] encryptedAESKey = new byte[aesLength];
                    in.readFully(encryptedAESKey);
                    byte[] aesKeyBytes = CryptoUtils.decryptRSA(encryptedAESKey, privateKey);
                    SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

                    // Receive encrypted credentials
                    int credLength = in.readInt();
                    byte[] encryptedCred = new byte[credLength];
                    in.readFully(encryptedCred);
                    String decrypted = new String(CryptoUtils.decryptAES(encryptedCred, aesKey));

                    // Parse credentials
                    String[] parts = decrypted.split(":");
                    String username = parts.length >= 1 ? parts[0].trim() : "unknown";
                    String password = parts.length >= 2 ? parts[1].trim() : "";
                    long timestamp = parts.length >= 3 ? Long.parseLong(parts[2]) : 0L;

                    // Check for replay attack
                    long currentTime = Instant.now().toEpochMilli();
                    if (Math.abs(currentTime - timestamp) > TIMESTAMP_WINDOW) {
                        NetUtils.logServer("Rejected due to timestamp mismatch.");
                        String errorMsg = "Rejected: Timestamp outside allowed window.";
                        NetUtils.logResponse("User '" + username + "' → " + errorMsg);
                        byte[] encrypted = CryptoUtils.encryptAES(errorMsg.getBytes(), aesKey);
                        out.writeInt(encrypted.length);
                        out.write(encrypted);
                        continue;
                    }

                    // Validate credentials
                    String response = UserValidator.isValid(username + ":" + password);
                    NetUtils.logServer("Received credentials from: " + username);
                    NetUtils.logResponse("User '" + username + "' → " + response);

                    byte[] encryptedResponse = CryptoUtils.encryptAES(response.getBytes(), aesKey);
                    out.writeInt(encryptedResponse.length);
                    out.write(encryptedResponse);

                } catch (Exception e) {
                    NetUtils.logServer("Client error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            NetUtils.logServer("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static PrivateKey loadPrivateKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}

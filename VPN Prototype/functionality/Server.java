package functionality;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Server {

    private static final int PORT = 9999;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            NetUtils.logServer("VPN Server started on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    NetUtils.logServer("Client connected");

                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                    // Load RSA private key
                    PrivateKey privateKey = loadPrivateKey("keys/private.key");

                    // Receive AES key encrypted with RSA
                    int aesLength = in.readInt();
                    byte[] encryptedAESKey = new byte[aesLength];
                    in.readFully(encryptedAESKey);
                    byte[] aesKeyBytes = CryptoUtils.decryptRSA(encryptedAESKey, privateKey);
                    SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

                    // Receive encrypted credentials
                    int credLength = in.readInt();
                    byte[] encryptedCred = new byte[credLength];
                    in.readFully(encryptedCred);
                    String credentials = new String(CryptoUtils.decryptAES(encryptedCred, aesKey));

                    // Validate credentials
                    String response = UserValidator.isValid(credentials);
                    String username = credentials.contains(":") ? credentials.split(":")[0].trim() : "unknown";

                    // Logging
                    NetUtils.logServer("Received credentials from: " + username);
                    NetUtils.logResponse("User '" + username + "' → " + response);

                    // Encrypt and send response
                    byte[] encryptedResponse = CryptoUtils.encryptAES(response.getBytes(), aesKey);
                    out.writeInt(encryptedResponse.length);
                    out.write(encryptedResponse);

                } catch (Exception e) {
                    NetUtils.logServer("Client handling error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            NetUtils.logServer("Server startup error: " + e.getMessage());
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

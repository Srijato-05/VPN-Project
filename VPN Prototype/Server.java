import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class Server {
    private static final int PORT = 9999;
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String LOG_FILE = "server_log.txt";

    public static void main(String[] args) {
        try {
            // Generate RSA key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Save public key in X.509 format
            byte[] publicKeyBytes = publicKey.getEncoded();
            Files.write(Paths.get(PUBLIC_KEY_FILE), publicKeyBytes);
            log("[Server] " + LocalTime.now() + " - Public key written to " + PUBLIC_KEY_FILE);

            // Start server
            ServerSocket serverSocket = new ServerSocket(PORT);
            log("[Server] " + LocalTime.now() + " - VPN Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("[Server] " + LocalTime.now() + " - Client connected: " + clientSocket.getInetAddress());

                // Handle client in new thread
                new Thread(() -> handleClient(clientSocket, privateKey)).start();
            }

        } catch (Exception e) {
            log("[Server] " + LocalTime.now() + " - Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, PrivateKey privateKey) {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ) {
            // Read encrypted AES key
            int keyLength = in.readInt();
            byte[] encryptedAESKey = new byte[keyLength];
            in.readFully(encryptedAESKey);

            // Decrypt AES key with RSA
            byte[] aesKeyBytes = decryptRSA(encryptedAESKey, privateKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
            log("[Server] AES key received and decrypted");

            // Read encrypted credentials
            int credLength = in.readInt();
            byte[] encryptedCreds = new byte[credLength];
            in.readFully(encryptedCreds);

            // Decrypt credentials
            String credentials = decryptAES(encryptedCreds, aesKey);
            log("[Server] Credentials received: " + credentials);

            // Simulate authentication
            String response;
            if ("srijato:sdas05".equals(credentials)) {
                response = "Authentication successful. Welcome to the VPN.";
            } else {
                response = "Authentication failed. Invalid username or password.";
            }

            // Encrypt response
            byte[] encryptedResponse = encryptAES(response.getBytes(), aesKey);
            out.writeInt(encryptedResponse.length);
            out.write(encryptedResponse);
            log("[Server] Response sent to client");

        } catch (Exception e) {
            log("[Server] Client handling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] decryptRSA(byte[] data, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private static String decryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(data);
        return new String(decryptedBytes);
    }

    private static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    // Log to console and file
    private static void log(String message) {
        System.out.println(message);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write("[" + LocalDateTime.now() + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Server] Failed to write log: " + e.getMessage());
        }
    }
}

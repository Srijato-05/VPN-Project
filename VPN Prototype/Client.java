import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Scanner;
import javax.crypto.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String LOG_FILE = "client_log.txt";
    private static final String RESPONSE_LOG_FILE = "response_log.txt";

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            log("[Client] " + LocalTime.now() + " - Connected to VPN Server");

            // Load server's public key
            log("[Client] Loading server's public key...");
            PublicKey publicKey = loadPublicKey(PUBLIC_KEY_FILE);

            // Generate AES key
            log("[Client] Generating AES key...");
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();

            // Encrypt AES key with RSA and send
            log("[Client] Encrypting AES key with server's public key...");
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), publicKey);
            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);

            // Get user credentials
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            String credentials = username + ":" + password;

            // Encrypt credentials with AES and send
            log("[Client] Encrypting credentials with AES...");
            byte[] encryptedCredentials = encryptAES(credentials.getBytes(), aesKey);
            out.writeInt(encryptedCredentials.length);
            out.write(encryptedCredentials);

            // Receive encrypted response
            int responseLength = in.readInt();
            byte[] encryptedResponse = new byte[responseLength];
            in.readFully(encryptedResponse);

            // Decrypt response
            log("[Client] Decrypting server response...");
            String decryptedResponse = decryptAES(encryptedResponse, aesKey);
            System.out.println("[Client] " + decryptedResponse);
            log("[Client] Server response: " + decryptedResponse);

            // Log decrypted response separately
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESPONSE_LOG_FILE, true))) {
                writer.write("[" + LocalDateTime.now() + "] " + decryptedResponse);
                writer.newLine();
            } catch (IOException e) {
                log("[Client] Failed to log response: " + e.getMessage());
            }

        } catch (Exception e) {
            log("[Client] " + LocalTime.now() + " - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Logging utility
    private static void log(String message) {
        System.out.println(message);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write("[" + LocalDateTime.now() + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Client] Failed to write log: " + e.getMessage());
        }
    }

    // Load server's RSA public key
    private static PublicKey loadPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    // RSA Encryption
    private static byte[] encryptRSA(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    // AES Encryption
    private static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    // AES Decryption
    private static String decryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(data);
        return new String(decryptedBytes);
    }
}

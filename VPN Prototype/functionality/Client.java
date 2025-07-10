package functionality;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.crypto.SecretKey;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final String HISTORY_FILE = "data/login_history.txt";
    private static final String PUBLIC_KEY_PATH = "keys/public.key";

    /**
     * Sends encrypted credentials to the server and receives a secure response.
     */
    public static String sendCredentials(String username, String password) throws Exception {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            String credentials = username + ":" + password;

            // Step 1: Generate AES session key
            SecretKey aesKey = CryptoUtils.generateKey("client-session");

            // Step 2: Encrypt AES key using server's RSA public key
            PublicKey serverPublicKey = loadPublicKey(PUBLIC_KEY_PATH);
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), serverPublicKey);

            // Step 3: Encrypt the credentials using AES
            byte[] encryptedCred = CryptoUtils.encryptAES(credentials.getBytes(), aesKey);

            // Step 4: Send encrypted AES key and credentials
            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);
            out.writeInt(encryptedCred.length);
            out.write(encryptedCred);

            // Step 5: Receive and decrypt server response
            int responseLen = in.readInt();
            byte[] encryptedResponse = new byte[responseLen];
            in.readFully(encryptedResponse);
            String serverReply = new String(CryptoUtils.decryptAES(encryptedResponse, aesKey));

            // Log response and save history
            NetUtils.logClient("Sent credentials for user: " + username);
            NetUtils.logResponse("Server replied: " + serverReply);
            saveToHistory(username);

            return serverReply;
        } catch (Exception e) {
            NetUtils.logClient("sendCredentials failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retry mechanism for connection (up to 3 attempts)
     */
    public static void retrySend(String username, String password) throws Exception {
        int attempts = 0;
        while (attempts < 3) {
            try {
                String result = sendCredentials(username, password);
                System.out.println("Server: " + result);
                if (result.toLowerCase().contains("success")) break;
                attempts++;
            } catch (IOException ex) {
                NetUtils.logClient("Retry " + (attempts + 1) + ": " + ex.getMessage());
                attempts++;
                if (attempts == 3)
                    throw new IOException("Maximum retry attempts reached.");
                Thread.sleep(1000);  // delay before retry
            }
        }
    }

    /**
     * Loads the server’s RSA public key from file.
     */
    public static PublicKey loadPublicKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * Encrypts byte array using RSA public key.
     */
    private static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Loads saved usernames from login history.
     */
    public static List<String> loadLoginHistory() {
        List<String> list = new ArrayList<>();
        try {
            Files.createDirectories(Paths.get("data"));
            File file = new File(HISTORY_FILE);
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        list.add(line.trim());
                }
            }
        } catch (IOException e) {
            NetUtils.logClient("Failed to load login history: " + e.getMessage());
        }
        if (list.isEmpty()) list.add("admin");  // fallback user
        return list;
    }

    /**
     * Saves new usernames to login history.
     */
    public static void saveToHistory(String username) {
        try {
            List<String> history = loadLoginHistory();
            if (!history.contains(username)) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
                    bw.write(username);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            NetUtils.logClient("Could not save login history: " + e.getMessage());
        }
    }
}

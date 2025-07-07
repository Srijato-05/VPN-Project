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

    public static String sendCredentials(String username, String password) throws Exception {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            String credentials = username + ":" + password;

            // AES session key
            SecretKey aesKey = CryptoUtils.generateKey("client-session");

            // Encrypt AES key with server public key
            PublicKey serverPublicKey = loadPublicKey("keys/public.key");
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), serverPublicKey);

            // Encrypt credentials
            byte[] encryptedCred = CryptoUtils.encryptAES(credentials.getBytes(), aesKey);

            // Send AES key
            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);

            // Send credentials
            out.writeInt(encryptedCred.length);
            out.write(encryptedCred);

            // Receive and decrypt response
            int responseLen = in.readInt();
            byte[] encryptedResponse = new byte[responseLen];
            in.readFully(encryptedResponse);
            String serverReply = new String(CryptoUtils.decryptAES(encryptedResponse, aesKey));

            // Logging
            NetUtils.logClient("Sent credentials for user: " + username);
            NetUtils.logResponse("User '" + username + "' → " + serverReply);

            return serverReply;

        } catch (IOException e) {
            NetUtils.logClient("Connection or I/O error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            NetUtils.logClient("Encryption/Auth error: " + e.getMessage());
            throw e;
        }
    }

    public static void retrySend(String username, String password) throws Exception {
        int attempts = 0;
        while (attempts < 3) {
            try {
                String result = sendCredentials(username, password);
                if (result.toLowerCase().contains("success")) break;
                else attempts++;
            } catch (IOException e) {
                NetUtils.logClient("Retry " + (attempts + 1) + ": " + e.getMessage());
                attempts++;
                if (attempts == 3)
                    throw new IOException("Maximum retry attempts reached.");
                Thread.sleep(1000);
            }
        }
    }

    public static PublicKey loadPublicKey(String filePath) throws Exception {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (IOException e) {
            NetUtils.logClient("Could not read public key: " + e.getMessage());
            throw e;
        }
    }

    private static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

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
        if (list.isEmpty()) list.add("admin"); // fallback
        return list;
    }

    public static void saveToHistory(String username) {
        try {
            Files.createDirectories(Paths.get("data"));
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

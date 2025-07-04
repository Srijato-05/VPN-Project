package functionality;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9999;

    // ✅ GUI-compatible method
    public static String sendCredentials(String username, String password) throws Exception {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

                String credentials = username + ":" + password;

            // Generate AES key
            SecretKey aesKey = CryptoUtils.generateKey("client-session");

            // Encrypt AES key using server's public key
            PublicKey serverPublicKey = loadPublicKey("keys/public.key");
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), serverPublicKey);

            // Encrypt credentials using AES
            byte[] encryptedCred = CryptoUtils.encryptAES(credentials.getBytes(), aesKey);

            // Send encrypted AES key
            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);

            // Send encrypted credentials
            out.writeInt(encryptedCred.length);
            out.write(encryptedCred);

            // Receive encrypted response
            int responseLen = in.readInt();
            byte[] encryptedResponse = new byte[responseLen];
            in.readFully(encryptedResponse);

            return new String(CryptoUtils.decryptAES(encryptedResponse, aesKey));
        }
    }

    // ✅ CLI fallback (optional)
    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Enter username: ");
            String username = br.readLine();

            System.out.print("Enter password: ");
            String password = br.readLine();

            String result = sendCredentials(username, password);
            System.out.println("Server response: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Load RSA public key from file
    private static PublicKey loadPublicKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    // ✅ RSA encryption for AES key
    private static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
}

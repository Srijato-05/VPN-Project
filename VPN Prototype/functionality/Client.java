package functionality;

import static functionality.CryptoUtils.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import javax.crypto.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9999;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            NetUtils.logClient("Connected to VPN Server");

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            NetUtils.logClient("Loading server's public key...");
            PublicKey publicKey = loadPublicKey("keys/public.key");

            NetUtils.logClient("Generating AES key...");
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();

            NetUtils.logClient("Encrypting AES key with server's public key...");
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), publicKey);
            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            String credentials = username + ":" + password;

            NetUtils.logClient("Encrypting credentials with AES...");
            byte[] encryptedCredentials = encryptAES(credentials.getBytes(), aesKey);
            out.writeInt(encryptedCredentials.length);
            out.write(encryptedCredentials);

            int responseLength = in.readInt();
            byte[] encryptedResponse = new byte[responseLength];
            in.readFully(encryptedResponse);

            NetUtils.logClient("Decrypting server response...");
            String decryptedResponse = new String(decryptAES(encryptedResponse, aesKey));
            System.out.println("[Client] " + decryptedResponse);
            NetUtils.logClient("Server response: " + decryptedResponse);
            NetUtils.logClientResponse(decryptedResponse);

        } catch (Exception e) {
            NetUtils.logClient("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static PublicKey loadPublicKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}

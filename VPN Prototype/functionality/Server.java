package functionality;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class Server {
    private static final int PORT = 9999;

    public static void main(String[] args) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            File keyDir = new File("keys");
            if (!keyDir.exists()) keyDir.mkdirs();
            try (FileOutputStream fos = new FileOutputStream("keys/public.key")) {
                fos.write(publicKey.getEncoded());
            }

            ServerSocket serverSocket = new ServerSocket(PORT);
            NetUtils.logServer("VPN Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                NetUtils.logServer("Client connected");

                new Thread(() -> handleClient(socket, privateKey)).start();
            }
        } catch (Exception e) {
            NetUtils.logServer("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, PrivateKey privateKey) {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            int keyLength = in.readInt();
            byte[] encryptedAESKey = new byte[keyLength];
            in.readFully(encryptedAESKey);

            byte[] aesKeyBytes = CryptoUtils.decryptRSA(encryptedAESKey, privateKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

            int credLength = in.readInt();
            byte[] encryptedCred = new byte[credLength];
            in.readFully(encryptedCred);
            String credentials = new String(CryptoUtils.decryptAES(encryptedCred, aesKey));

            String response = UserValidator.isValid(credentials) ? "Access Granted" : "Access Denied";
            NetUtils.logServer("Response to client: " + response);

            byte[] encryptedResponse = CryptoUtils.encryptAES(response.getBytes(), aesKey);
            out.writeInt(encryptedResponse.length);
            out.write(encryptedResponse);

        } catch (Exception e) {
            NetUtils.logServer("Client handling error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

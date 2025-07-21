package functionality;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final String HISTORY_FILE = "data/login_history.txt";

    public static String sendCredentials(String username, String password) throws Exception {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())) {

            long timestamp = Instant.now().toEpochMilli();
            String credentials = username + ":" + password + ":" + timestamp;

            SecretKey aesKey = CryptoUtils.generateKey("client-session");
            PublicKey serverPublicKey = loadPublicKey("keys/public.key");
            byte[] encryptedAESKey = encryptRSA(aesKey.getEncoded(), serverPublicKey);
            byte[] encryptedCred = CryptoUtils.encryptAES(credentials.getBytes(), aesKey);

            out.writeInt(encryptedAESKey.length);
            out.write(encryptedAESKey);
            out.writeInt(encryptedCred.length);
            out.write(encryptedCred);

            int responseLen = in.readInt();
            byte[] encryptedResponse = new byte[responseLen];
            in.readFully(encryptedResponse);

            String serverReply = new String(CryptoUtils.decryptAES(encryptedResponse, aesKey));

            NetUtils.logClient("Sent credentials for user: " + username + " with timestamp: " + timestamp);
            NetUtils.logResponse("Server replied: " + serverReply);

            return serverReply;
        }
    }

    public static void retrySend(String username, String password) throws Exception {
        int attempts = 0;
        while (attempts < 3) {
            try {
                String result = sendCredentials(username, password);
                if (result.toLowerCase().contains("success")) break;
                else attempts++;
            } catch (IOException ex) {
                NetUtils.logClient("Retry " + (attempts + 1) + ": " + ex.getMessage());
                attempts++;
                if (attempts == 3)
                    throw new IOException("Maximum retry attempts reached.");
                Thread.sleep(1000);
            }
        }
    }

    public static PublicKey loadPublicKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
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
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null)
                    list.add(line.trim());
                br.close();
            }
        } catch (IOException e) {
            NetUtils.logClient("Failed to load login history: " + e.getMessage());
        }
        if (list.isEmpty()) list.add("admin"); // fallback
        return list;
    }

    public static void saveToHistory(String username) {
        try {
            List<String> history = loadLoginHistory();
            if (!history.contains(username)) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(HISTORY_FILE, true));
                bw.write(username);
                bw.newLine();
                bw.close();
            }
        } catch (IOException e) {
            NetUtils.logClient("Could not save login history: " + e.getMessage());
        }
    }

    // 🔒 Launch OpenVPN process with selected config file
    public static void launchOpenVPN(String configPath) throws IOException {
        String openVPNExecutable = "C:\\Program Files\\OpenVPN\\bin\\openvpn.exe";

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            NetUtils.logClient("Config file not found: " + configPath);
            throw new FileNotFoundException("VPN config not found.");
        }

        ProcessBuilder builder = new ProcessBuilder(
                openVPNExecutable,
                "--config", configFile.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        NetUtils.logClient("OpenVPN launched with config: " + configPath);

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    NetUtils.logClient("[OpenVPN] " + line);
                }
            } catch (IOException e) {
                NetUtils.logClient("Failed to read OpenVPN output: " + e.getMessage());
            }
        }).start();
    }
}

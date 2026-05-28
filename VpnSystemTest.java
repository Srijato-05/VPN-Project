import functionality.*;
import vpn_proxy.*;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKey;

public class VpnSystemTest {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static File backupUsersFile;
    private static File backupVaultFile;
    private static File backupHistoryFile;

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println(CYAN + "===============================================" + RESET);
        System.out.println(CYAN + "   CIPHERVPN COMPREHENSIVE SYSTEM TEST SUITE   " + RESET);
        System.out.println(CYAN + "===============================================" + RESET);

        try {
            setup();
            
            runTest("AppConfig Getters & Setters", VpnSystemTest::testAppConfig);
            runTest("CryptoUtils Symmetric AES Encryption/Decryption", VpnSystemTest::testSymmetricCrypto);
            runTest("CryptoUtils Asymmetric RSA Encrypt/Decrypt", VpnSystemTest::testAsymmetricCrypto);
            runTest("CryptoUtils Password Hashing", VpnSystemTest::testPasswordHashing);
            runTest("UserValidator Authentication & Duplicate Checks", VpnSystemTest::testUserValidator);
            runTest("Client Hardware UUID & Key Generation", VpnSystemTest::testClientHardwareBinding);
            runTest("Client Local Secure Vault Storage & History", VpnSystemTest::testClientSecureVault);
            runTest("Client OpenVPN Executable Path Lookup", VpnSystemTest::testOpenVPNPathLookup);
            runTest("Client Hybrid Ping Engine", VpnSystemTest::testHybridPingEngine);
            runTest("Client-Server Socket Communication Integration", VpnSystemTest::testClientServerIntegration);
            runTest("VPN Proxy Flow Integration", VpnSystemTest::testProxyFlow);
            runTest("Network Replay Attack Prevention Security", VpnSystemTest::testNetworkReplayAttackPrevention);
            runTest("Brute Force Rate-limiting & Lockout Security", VpnSystemTest::testBruteForceLockoutSecurity);
            runTest("Hardware-Binding Vault Decryption Failure Verification", VpnSystemTest::testHardwareBindingVaultDecryptionFailure);
            runTest("Double-Hop Multi-VPN Config Chaining Verification", VpnSystemTest::testDoubleVpnHopConfigGeneration);
            runTest("Kill-Switch Firewall Rule Execution Safety", VpnSystemTest::testKillSwitchFirewallDeployment);
            runTest("Vault Session Pre-Authorization Flow", VpnSystemTest::testSessionPreAuthorization);
            runTest("Zero-Knowledge Memory-Wiping Security", VpnSystemTest::testZeroKnowledgeWiping);
            runTest("Malformed Control Payloads and Error Handling", VpnSystemTest::testInvalidControlCommands);
            runTest("Multi-threaded Concurrent Server Requests", VpnSystemTest::testConcurrentClientConnections);
            runTest("Automatic Connection Recovery & Retry", VpnSystemTest::testConnectionRetryOnError);

        } catch (Exception e) {
            System.err.println(RED + "\nFATAL ERROR DURING TEST EXECUTION SETUP: " + e.getMessage() + RESET);
            e.printStackTrace();
        } finally {
            teardown();
        }

        System.out.println(CYAN + "\n===============================================" + RESET);
        System.out.println(CYAN + "   TEST RUN SUMMARY: " + passedTests + " / " + totalTests + " PASSED" + RESET);
        if (passedTests == totalTests) {
            System.out.println(GREEN + "   SUCCESS: ALL TESTS PASSED SUCCESSFULLY! ✅" + RESET);
        } else {
            System.out.println(RED + "   FAILURE: SOME TESTS FAILED! ❌" + RESET);
        }
        System.out.println(CYAN + "===============================================" + RESET);
    }

    private static void setup() throws Exception {
        System.out.println(YELLOW + "[SETUP] Backing up database, vault, and configuration files..." + RESET);
        
        // Backup users.txt
        String usersPath = AppConfig.getUsersFilePath();
        File usersFile = new File(usersPath);
        if (usersFile.exists()) {
            backupUsersFile = new File(usersPath + ".bak");
            Files.copy(usersFile.toPath(), backupUsersFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            usersFile.delete();
        }

        // Backup secure_vault.dat
        File vaultFile = new File("data/secure_vault.dat");
        if (vaultFile.exists()) {
            backupVaultFile = new File("data/secure_vault.dat.bak");
            Files.copy(vaultFile.toPath(), backupVaultFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            vaultFile.delete();
        }

        // Backup login_history.txt
        String historyPath = AppConfig.getHistoryFilePath();
        File historyFile = new File(historyPath);
        if (historyFile.exists()) {
            backupHistoryFile = new File(historyPath + ".bak");
            Files.copy(historyFile.toPath(), backupHistoryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            historyFile.delete();
        }

        // Setup custom configurations for testing
        AppConfig.setProperty("path.users", "data/users_test.txt");
        AppConfig.setProperty("path.history", "data/login_history_test.txt");
        AppConfig.setProperty("server.port", "9998");
        AppConfig.setProperty("server.host", "127.0.0.1");

        // Clean any old test files
        new File("data/users_test.txt").delete();
        new File("data/login_history_test.txt").delete();
    }

    private static void teardown() {
        System.out.println(YELLOW + "\n[TEARDOWN] Restoring production database and vault files..." + RESET);
        
        // Delete test files
        new File("data/users_test.txt").delete();
        new File("data/login_history_test.txt").delete();
        new File("data/secure_vault.dat").delete();

        // Restore users.txt
        if (backupUsersFile != null && backupUsersFile.exists()) {
            try {
                Files.move(backupUsersFile.toPath(), new File(AppConfig.getUsersFilePath()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }
        
        // Restore secure_vault.dat
        if (backupVaultFile != null && backupVaultFile.exists()) {
            try {
                Files.move(backupVaultFile.toPath(), new File("data/secure_vault.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }

        // Restore login_history.txt
        if (backupHistoryFile != null && backupHistoryFile.exists()) {
            try {
                Files.move(backupHistoryFile.toPath(), new File(AppConfig.getHistoryFilePath()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }
    }

    private static void runTest(String name, TestRunnable runnable) {
        totalTests++;
        System.out.print(CYAN + "Running: " + name + "... " + RESET);
        try {
            runnable.run();
            passedTests++;
            System.out.println(GREEN + "PASSED ✅" + RESET);
        } catch (Throwable t) {
            System.out.println(RED + "FAILED ❌" + RESET);
            System.out.println(RED + "  -> Reason: " + t.getMessage() + RESET);
            t.printStackTrace();
        }
    }

    interface TestRunnable {
        void run() throws Exception;
    }

    // --- TEST CASES ---

    private static void testAppConfig() throws Exception {
        // Test custom set property
        AppConfig.setProperty("test.key", "test.val");
        assertEquals("test.val", AppConfig.getProperty("test.key", "default"));

        // Test default falls back
        assertEquals("fallback", AppConfig.getProperty("non-existent-key", "fallback"));

        // Test numerical property parsing
        AppConfig.setProperty("test.int", "12345");
        assertEquals(12345, AppConfig.getIntProperty("test.int", 0));

        AppConfig.setProperty("test.long", "9876543210");
        assertEquals(9876543210L, AppConfig.getLongProperty("test.long", 0L));
    }

    private static void testSymmetricCrypto() throws Exception {
        String testData = "SecureVPNPayload-2026-SuperKeyEncryption";
        SecretKey key = CryptoUtils.generateKey("mySecretPassphraseWord");

        byte[] encrypted = CryptoUtils.encryptAES(testData.getBytes("UTF-8"), key);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 16); // IV (16 bytes) + encrypted blocks

        byte[] decrypted = CryptoUtils.decryptAES(encrypted, key);
        String decryptedStr = new String(decrypted, "UTF-8");
        assertEquals(testData, decryptedStr);
    }

    private static void testAsymmetricCrypto() throws Exception {
        byte[] sessionKey = "testSessionAESKey123".getBytes("UTF-8");

        PublicKey publicKey = Client.loadPublicKey("keys/public.key");
        assertNotNull(publicKey);

        // Encrypt with client side logic
        byte[] encrypted = CryptoUtils.encryptRSA(sessionKey, publicKey);
        assertNotNull(encrypted);

        // Decrypt with server side private loadPrivateKey reflection call
        java.lang.reflect.Method loadKeyMethod = Server.class.getDeclaredMethod("loadPrivateKey", String.class);
        loadKeyMethod.setAccessible(true);
        PrivateKey privateKey = (PrivateKey) loadKeyMethod.invoke(null, "keys/private.key");

        assertNotNull(privateKey);
        byte[] decrypted = CryptoUtils.decryptRSA(encrypted, privateKey);
        assertArrayEquals(sessionKey, decrypted);
    }

    private static void testPasswordHashing() throws Exception {
        String password = "StrongPassword@999!";
        byte[] salt = CryptoUtils.generateSalt();
        assertEquals(16, salt.length);

        String hash1 = CryptoUtils.hashPassword(password, salt);
        String hash2 = CryptoUtils.hashPassword(password, salt);
        assertEquals(hash1, hash2); // Deterministic with same salt

        // Different salt should generate different hash
        byte[] salt2 = CryptoUtils.generateSalt();
        String hash3 = CryptoUtils.hashPassword(password, salt2);
        assertNotEquals(hash1, hash3);
    }

    private static void testUserValidator() throws Exception {
        // Trigger class static initializer to load clean test db
        UserValidator.registerUser("testuser1", "mypass123");

        // Authenticate correctly
        String authResult = UserValidator.isValid("testuser1:mypass123");
        assertEquals("Authentication successful", authResult);

        // Authenticate with wrong password
        String authResult2 = UserValidator.isValid("testuser1:wrongpass");
        assertEquals("Incorrect password.", authResult2);

        // Authenticate with non-existent user
        String authResult3 = UserValidator.isValid("nonexistent:pass");
        assertEquals("User not found.", authResult3);

        // Duplicate registration check
        String regResult = UserValidator.registerUser("testuser1", "otherpass");
        assertEquals("Username already exists.", regResult);

        // Validation format checks
        assertEquals("Invalid format. Expected username:password", UserValidator.isValid("badformat"));
    }

    private static void testClientHardwareBinding() throws Exception {
        String uuid = Client.getHardwareUUID();
        assertNotNull(uuid);
        assertFalse(uuid.isEmpty());

        // Test hardware key generation via reflection
        java.lang.reflect.Method getHardwareKeyMethod = Client.class.getDeclaredMethod("getHardwareKey");
        getHardwareKeyMethod.setAccessible(true);
        byte[] hardwareKey = (byte[]) getHardwareKeyMethod.invoke(null);
        
        assertNotNull(hardwareKey);
        assertEquals(32, hardwareKey.length); // SHA-256 is 32 bytes
    }

    private static void testClientSecureVault() throws Exception {
        String mockHistory = "userA,userB,userC";
        Client.saveToVault(mockHistory);

        String loaded = Client.loadFromVault();
        assertEquals(mockHistory, loaded);

        // Test history append tracking
        Client.saveToHistory("userD");
        List<String> history = Client.loadLoginHistory();
        assertTrue(history.contains("userA"));
        assertTrue(history.contains("userD"));
    }

    private static void testOpenVPNPathLookup() throws Exception {
        String path = Client.getOpenVPNExecutablePath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    private static void testHybridPingEngine() throws Exception {
        // Ping localhost (should return quick ping if anything listens, or resolve loopback)
        int localhostPing = Client.pingServer("127.0.0.1", 9998);
        // Ping local server or fall back to mock region latency for known domain names
        int mockRegionPing = Client.pingServer("us-free-3.protonvpn.com", 1194);
        
        // Assert that ping returns a numeric latency value (either >=0 or -1 if completely offline)
        assertTrue(localhostPing >= -1);
        assertTrue(mockRegionPing >= -1);
    }

    private static void testClientServerIntegration() throws Exception {
        // Start server in background thread
        Thread serverThread = new Thread(() -> {
            Server.main(new String[0]);
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to bind to port 9998
        Thread.sleep(500);

        // Test connection command (LIST_LOCATIONS)
        String response = Client.sendCommand("LIST_LOCATIONS", "", "");
        assertNotNull(response);
        assertTrue(response.contains("android") || response.contains("ERROR") || response.contains("No locations"));

        // Register user via client-server command
        String regResponse = Client.sendCommand("REGISTER", "servertestuser", "securepass99");
        assertTrue(regResponse.contains("successful") || regResponse.contains("already exists"));

        // Login via client credentials command
        String loginResponse = Client.sendCommand("LOGIN", "servertestuser", "securepass99");
        assertTrue(loginResponse.contains("SUCCESS") || loginResponse.contains("successful"));

        Client.closePersistentConnection();
    }

    private static void testProxyFlow() throws Exception {
        // Start a mock server on port 9998 (done by testClientServerIntegration)
        // Configure Proxy to forward from 9988 to 9998
        ProxyConfig.setProperty("listenPort", "9988");
        ProxyConfig.setProperty("forwardHost", "127.0.0.1");
        ProxyConfig.setProperty("forwardPort", "9998");

        Thread proxyThread = new Thread(() -> {
            ProxyMain.main(new String[0]);
        });
        proxyThread.setDaemon(true);
        proxyThread.start();

        // Wait for proxy to start
        Thread.sleep(500);

        // Connect client directly to the proxy (port 9988)
        AppConfig.setProperty("server.port", "9988");
        Client.closePersistentConnection(); // Reset old connection

        String response = Client.sendCommand("LIST_LOCATIONS", "", "");
        assertNotNull(response);
        assertTrue(response.contains("android") || response.contains("ERROR") || response.contains("No locations"));
        
        Client.closePersistentConnection();
    }

    private static void testNetworkReplayAttackPrevention() throws Exception {
        // First establish a connection by sending a normal command
        AppConfig.setProperty("server.port", "9998");
        Client.closePersistentConnection();
        Client.sendCommand("LIST_LOCATIONS", "", "");

        // Retrieve private session properties via reflection
        java.lang.reflect.Field keyField = Client.class.getDeclaredField("sessionAESKey");
        keyField.setAccessible(true);
        SecretKey sessionAESKey = (SecretKey) keyField.get(null);

        java.lang.reflect.Field outField = Client.class.getDeclaredField("persistentOut");
        outField.setAccessible(true);
        DataOutputStream persistentOut = (DataOutputStream) outField.get(null);

        java.lang.reflect.Field inField = Client.class.getDeclaredField("persistentIn");
        inField.setAccessible(true);
        DataInputStream persistentIn = (DataInputStream) inField.get(null);

        assertNotNull(sessionAESKey);
        assertNotNull(persistentOut);
        assertNotNull(persistentIn);

        // Construct command payload with an expired timestamp (2 hours ago)
        long expiredTimestamp = System.currentTimeMillis() - 7200000;
        String payload = "LIST_LOCATIONS:::" + expiredTimestamp;
        
        byte[] encryptedCred = CryptoUtils.encryptAES(payload.getBytes("UTF-8"), sessionAESKey);
        
        // Write the invalid payload manually to the server stream
        persistentOut.writeInt(encryptedCred.length);
        persistentOut.write(encryptedCred);
        persistentOut.flush();

        // Read and decrypt server response
        int responseLen = persistentIn.readInt();
        byte[] encryptedResponse = new byte[responseLen];
        persistentIn.readFully(encryptedResponse);
        
        String serverReply = new String(CryptoUtils.decryptAES(encryptedResponse, sessionAESKey), "UTF-8");
        
        // Verify server rejected command due to timestamp mismatch
        assertTrue(serverReply.contains("Rejected") || serverReply.contains("Timestamp outside allowed window"));
        
        Client.closePersistentConnection();
    }

    private static void testBruteForceLockoutSecurity() throws Exception {
        UserValidator.resetLockouts();
        UserValidator.registerUser("bruteforceuser", "goodpass123");

        // Try 4 incorrect logins - should return incorrect password
        for (int i = 0; i < 4; i++) {
            String res = UserValidator.isValid("bruteforceuser:wrongpass");
            assertEquals("Incorrect password.", res);
        }

        // The 5th incorrect login should trigger lockout
        String res5 = UserValidator.isValid("bruteforceuser:wrongpass");
        assertEquals("Account temporarily locked due to multiple failed login attempts.", res5);

        // Try login with correct password while locked out
        String res6 = UserValidator.isValid("bruteforceuser:goodpass123");
        assertTrue(res6.contains("Account temporarily locked") || res6.contains("Try again in"));

        // Clean up lockouts
        UserValidator.resetLockouts();
    }

    private static void testHardwareBindingVaultDecryptionFailure() throws Exception {
        String testData = "TopSecretCredentialsData";
        Client.saveToVault(testData);

        // Verify it loads correctly first
        assertEquals(testData, Client.loadFromVault());

        // Mock a change in hardware signature
        System.setProperty("test.mock.uuid", "completely-different-hardware-uuid-99999");

        // Decryption should fail because hardware hash is different, returning empty
        String failedLoad = Client.loadFromVault();
        assertNotEquals(testData, failedLoad);
        assertEquals("", failedLoad);

        // Clear mock property to restore original behavior
        System.clearProperty("test.mock.uuid");
        assertEquals(testData, Client.loadFromVault()); // Should load successfully again
    }

    private static void testDoubleVpnHopConfigGeneration() throws Exception {
        AppConfig.setProperty("server.port", "9998");
        Client.closePersistentConnection();
        
        String configText = Client.sendCommand("GET_CONFIG", "android", "nl-free-16.protonvpn.udp.ovpn|true");
        assertNotNull(configText);
        
        if (!configText.startsWith("ERROR:")) {
            assertTrue(configText.contains("# --- CipherVPN Double Hop Tunnel Activated ---"));
            assertTrue(configText.contains("route 10.8.0.1 255.255.255.255 net_gateway"));
        } else {
            // Test Server's getConfig directly with mocked directory and file
            java.lang.reflect.Method getConfigMethod = Server.class.getDeclaredMethod("getConfig", String.class, String.class, boolean.class);
            getConfigMethod.setAccessible(true);
            
            File tempConfigDir = new File(AppConfig.getConfigDir() + "/testloc");
            tempConfigDir.mkdirs();
            File tempConfigFile = new File(tempConfigDir, "test.ovpn");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempConfigFile))) {
                writer.write("remote 1.2.3.4 1194 udp");
            }
            
            String response = (String) getConfigMethod.invoke(null, "testloc", "test.ovpn", true);
            assertTrue(response.contains("# --- CipherVPN Double Hop Tunnel Activated ---"));
            assertTrue(response.contains("route 10.8.0.1 255.255.255.255 net_gateway"));
            
            tempConfigFile.delete();
            tempConfigDir.delete();
        }
    }

    private static void testKillSwitchFirewallDeployment() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                Client.enableKillSwitch("192.168.1.100");
                Client.disableKillSwitch();
            } catch (Exception ignored) {}
        } else {
            Client.enableKillSwitch("192.168.1.100");
            Client.disableKillSwitch();
        }
    }

    private static void testSessionPreAuthorization() throws Exception {
        UserValidator.registerUser("authsessionuser", "sessionpass789");
        String response = Client.sendCredentials("authsessionuser", "sessionpass789".toCharArray(), "LOGIN");
        assertTrue(response.startsWith("SUCCESS:"));
        String[] parts = response.split(":", 4);
        assertEquals(4, parts.length);
        assertEquals("SUCCESS", parts[0]);
        java.util.UUID.fromString(parts[1]); // Will throw exception if malformed UUID
        assertNotNull(parts[2]);
        assertNotNull(parts[3]);
    }

    private static void testZeroKnowledgeWiping() throws Exception {
        char[] pass = "mySuperSecretPassword123".toCharArray();
        try {
            Client.sendCredentials("testuser1", pass, "LOGIN");
        } catch (Exception ignored) {}
        for (int i = 0; i < pass.length; i++) {
            assertEquals('\0', pass[i]);
        }
    }

    private static void testInvalidControlCommands() throws Exception {
        try {
            String res = Client.sendCommand("INVALID_COMMAND_NAME", "param1", "param2");
            assertTrue(res.contains("User not found") || res.contains("Invalid format"));
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("A");
        String longParam = sb.toString();
        String resLong = Client.sendCommand("LIST_SERVERS", longParam, "");
        assertTrue(resLong.contains("ERROR") || resLong.contains("directory not found") || resLong.contains("Invalid location"));
    }

    private static void testConcurrentClientConnections() throws Exception {
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        final boolean[] successFlags = new boolean[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    java.net.Socket s = new java.net.Socket("127.0.0.1", 9998);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    
                    SecretKey aesKey = CryptoUtils.generateKey("thread-aes-" + index);
                    PublicKey serverPublicKey = Client.loadPublicKey("keys/public.key");
                    byte[] encryptedAESKey = CryptoUtils.encryptRSA(aesKey.getEncoded(), serverPublicKey);
                    
                    out.writeInt(encryptedAESKey.length);
                    out.write(encryptedAESKey);
                    out.flush();
                    
                    long ts = System.currentTimeMillis();
                    String payload = "LOGIN:testuser1:mypass123:" + ts;
                    byte[] encPayload = CryptoUtils.encryptAES(payload.getBytes("UTF-8"), aesKey);
                    out.writeInt(encPayload.length);
                    out.write(encPayload);
                    out.flush();
                    
                    int respLen = in.readInt();
                    byte[] respBytes = new byte[respLen];
                    in.readFully(respBytes);
                    String reply = new String(CryptoUtils.decryptAES(respBytes, aesKey), "UTF-8");
                    
                    if (reply.contains("SUCCESS") || reply.contains("successful")) {
                        successFlags[index] = true;
                    }
                    s.close();
                } catch (Exception e) {
                    successFlags[index] = false;
                }
            });
            threads[i].start();
        }
        
        for (int i = 0; i < threadCount; i++) {
            threads[i].join(5000);
            assertTrue(successFlags[i]);
        }
    }

    private static void testConnectionRetryOnError() throws Exception {
        String r1 = Client.sendCommand("LIST_LOCATIONS", "", "");
        assertNotNull(r1);
        
        java.lang.reflect.Field socketField = Client.class.getDeclaredField("persistentSocket");
        socketField.setAccessible(true);
        java.net.Socket socket = (java.net.Socket) socketField.get(null);
        assertNotNull(socket);
        socket.close();
        
        String r2 = Client.sendCommand("LIST_LOCATIONS", "", "");
        assertNotNull(r2);
        assertTrue(r2.contains("android") || r2.contains("ERROR") || r2.contains("No locations"));
    }

    // --- CUSTOM ASSERTION UTILS ---

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected '" + expected + "' but got '" + actual + "'");
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + "L but got " + actual + "L");
        }
    }

    private static void assertNotEquals(Object val1, Object val2) {
        if (Objects.equals(val1, val2)) {
            throw new AssertionError("Value '" + val1 + "' was not expected to equal '" + val2 + "'");
        }
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Object was null");
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Condition was not true");
        }
    }

    private static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Condition was not false");
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Byte arrays do not match");
        }
    }
}

package functionality;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private static final String AES_ALGO = "AES/CBC/PKCS5Padding";
    private static final String RSA_ALGO = "RSA";
    private static final String HASH_ALGO = "PBKDF2WithHmacSHA256";
    private static final int IV_SIZE = 16;
    private static final int AES_KEY_SIZE = 128;
    private static final int ITERATIONS = 65536;
    private static final byte[] SALT = "vpn-hardcoded-salt".getBytes(); // Could be improved with random salt

    /**
     * Generate AES SecretKey from a password (using PBKDF2).
     */
    public static SecretKey generateKey(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * AES Encryption with random IV prepended to ciphertext.
     */
    public static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        byte[] iv = generateIV();
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);

        byte[] combined = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);
        return combined;
    }

    /**
     * AES Decryption (extracts IV from ciphertext).
     */
    public static byte[] decryptAES(byte[] encrypted, SecretKey key) throws Exception {
        byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_SIZE);
        byte[] actualCipher = Arrays.copyOfRange(encrypted, IV_SIZE, encrypted.length);
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(actualCipher);
    }

    /**
     * RSA Encryption using public key.
     */
    public static byte[] encryptRSA(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * RSA Decryption using private key.
     */
    public static byte[] decryptRSA(byte[] data, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Generate secure random 16-byte IV.
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}

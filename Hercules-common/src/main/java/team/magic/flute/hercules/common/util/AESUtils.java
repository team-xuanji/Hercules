package team.magic.flute.hercules.common.util;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * AES encryption and decryption utility class
 * Uses BouncyCastle library to implement AES256-GCM encryption
 *
 */
public class AESUtils {

    /** AES algorithm */
    private static final String AES_ALGORITHM = "AES";

    /** IV length (bytes) */
    private static final int IV_LENGTH = 12;

    /** GCM tag length (bits) */
    private static final int GCM_TAG_LENGTH = 128;

    /** Default key length - uses AES256 */
    private static final int DEFAULT_KEY_LENGTH = 256;

    /** AES256 key length (bytes) */
    private static final int AES256_KEY_LENGTH = 32;

    // Static initialization of BouncyCastle Provider
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * AES256-GCM encryption using BouncyCastle
     *
     * @param plainText plain text byte array
     * @param key key byte array
     * @param iv initialization vector byte array
     * @return encrypted byte array (including authentication tag)
     * @throws Exception thrown when encryption fails
     */
    private static byte[] encryptWithBouncyCastle(byte[] plainText, byte[] key, byte[] iv) throws Exception {
        GCMBlockCipher cipher = (GCMBlockCipher) GCMBlockCipher.newInstance(AESEngine.newInstance());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), GCM_TAG_LENGTH, iv);

        cipher.init(true, parameters);

        byte[] output = new byte[cipher.getOutputSize(plainText.length)];
        int len = cipher.processBytes(plainText, 0, plainText.length, output, 0);
        len += cipher.doFinal(output, len);

        // Return array with actual length
        byte[] result = new byte[len];
        System.arraycopy(output, 0, result, 0, len);
        return result;
    }

    /**
     * AES256-GCM decryption using BouncyCastle
     *
     * @param cipherText cipher text byte array (including authentication tag)
     * @param key key byte array
     * @param iv initialization vector byte array
     * @return decrypted byte array
     * @throws Exception thrown when decryption fails
     */
    private static byte[] decryptWithBouncyCastle(byte[] cipherText, byte[] key, byte[] iv) throws Exception {
        GCMBlockCipher cipher = (GCMBlockCipher) GCMBlockCipher.newInstance(AESEngine.newInstance());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), GCM_TAG_LENGTH, iv);

        cipher.init(false, parameters);

        byte[] output = new byte[cipher.getOutputSize(cipherText.length)];
        int len = cipher.processBytes(cipherText, 0, cipherText.length, output, 0);
        len += cipher.doFinal(output, len);

        // Return array with actual length
        byte[] result = new byte[len];
        System.arraycopy(output, 0, result, 0, len);
        return result;
    }

    /**
     * Validates key length - enforces AES256 usage
     *
     * @param keyBytes key byte array
     * @throws IllegalArgumentException thrown when key length is invalid
     */
    private static void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length != AES256_KEY_LENGTH) {
            throw new IllegalArgumentException("Invalid AES key length: " + keyBytes.length + " bytes. " +
                    "AES256 key must be 32 bytes (256 bits). Use generateKey() to create a valid key.");
        }
    }

    /**
     * Generates AES256 key
     *
     * @param keyLength key length (enforced 256-bit)
     * @return Base64 encoded key string
     * @throws RuntimeException thrown when key generation fails
     */
    public static String generateKey(int keyLength) {
        if (keyLength != 256) {
            throw new IllegalArgumentException("Only AES256 (256-bit) keys are supported. Use generateKey() for default AES256 key.");
        }
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyGenerator.init(keyLength);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES256 key", e);
        }
    }

    /**
     * Generates default AES256 key
     *
     * @return Base64 encoded key string
     */
    public static String generateKey() {
        return generateKey(DEFAULT_KEY_LENGTH);
    }

    /**
     * Generates random IV
     *
     * @return Base64 encoded IV string
     */
    public static String generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * AES256-GCM encryption (using BouncyCastle)
     *
     * @param plainText plain text
     * @param key Base64 encoded key
     * @param iv Base64 encoded initialization vector
     * @return Base64 encoded cipher text
     * @throws RuntimeException thrown when encryption fails
     */
    public static String encrypt(String plainText, String key, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            validateKeyLength(keyBytes);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);

            byte[] encrypted = encryptWithBouncyCastle(plainBytes, keyBytes, ivBytes);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES256-GCM encryption failed", e);
        }
    }

    /**
     * AES256-GCM decryption (using BouncyCastle)
     *
     * @param cipherText Base64 encoded cipher text
     * @param key Base64 encoded key
     * @param iv Base64 encoded initialization vector
     * @return plain text
     * @throws RuntimeException thrown when decryption fails
     */
    public static String decrypt(String cipherText, String key, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            validateKeyLength(keyBytes);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] cipherBytes = Base64.getDecoder().decode(cipherText);

            byte[] decrypted = decryptWithBouncyCastle(cipherBytes, keyBytes, ivBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES256-GCM decryption failed", e);
        }
    }

    /**
     * Convenient string encryption method (auto-generates IV)
     *
     * @param plainText plain text
     * @param key Base64 encoded key
     * @return result object containing IV and cipher text
     */
    public static EncryptResult encrypt(String plainText, String key) {
        String iv = generateIV();
        String cipherText = encrypt(plainText, key, iv);
        return new EncryptResult(cipherText, iv);
    }

    /**
     * Byte array encryption (using BouncyCastle)
     *
     * @param data byte array to be encrypted
     * @param key key byte array
     * @param iv initialization vector byte array
     * @return encrypted byte array
     * @throws RuntimeException thrown when encryption fails
     */
    public static byte[] encrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            validateKeyLength(key);
            return encryptWithBouncyCastle(data, key, iv);
        } catch (Exception e) {
            throw new RuntimeException("Byte array encryption failed", e);
        }
    }

    /**
     * Byte array decryption (using BouncyCastle)
     *
     * @param encryptedData encrypted byte array
     * @param key key byte array
     * @param iv initialization vector byte array
     * @return decrypted byte array
     * @throws RuntimeException thrown when decryption fails
     */
    public static byte[] decrypt(byte[] encryptedData, byte[] key, byte[] iv) {
        try {
            validateKeyLength(key);
            return decryptWithBouncyCastle(encryptedData, key, iv);
        } catch (Exception e) {
            throw new RuntimeException("Byte array decryption failed", e);
        }
    }

    /**
     * Byte array encryption using string key and IV (using BouncyCastle)
     *
     * @param data byte array to be encrypted
     * @param key Base64 encoded key
     * @param iv Base64 encoded initialization vector
     * @return encrypted byte array
     * @throws RuntimeException thrown when encryption fails
     */
    public static byte[] encrypt(byte[] data, String key, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            validateKeyLength(keyBytes);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            return encryptWithBouncyCastle(data, keyBytes, ivBytes);
        } catch (Exception e) {
            throw new RuntimeException(StrFormat.format("Byte array encryption failed, key[{}], iv[{}]",key,iv), e);
        }
    }

    /**
     * Byte array decryption using string key and IV (using BouncyCastle)
     *
     * @param cipherText Base64 encoded cipher text
     * @param key Base64 encoded key
     * @param iv Base64 encoded initialization vector
     * @return decrypted byte array
     * @throws RuntimeException thrown when decryption fails
     */
    public static byte[] decryptBytes(String cipherText, String key, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            validateKeyLength(keyBytes);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
            return decryptWithBouncyCastle(cipherBytes, keyBytes, ivBytes);
        } catch (Exception e) {
            throw new RuntimeException("Byte array decryption failed", e);
        }
    }

    /**
     * Byte array decryption using string key and IV (using BouncyCastle)
     *
     * @param encryptedData encrypted byte array
     * @param key Base64 encoded key
     * @param iv Base64 encoded initialization vector
     * @return decrypted byte array
     * @throws RuntimeException thrown when decryption fails
     */
    public static byte[] decrypt(byte[] encryptedData, String key, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            validateKeyLength(keyBytes);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            return decryptWithBouncyCastle(encryptedData, keyBytes, ivBytes);
        } catch (Exception e) {
            throw new RuntimeException("Byte array decryption failed", e);
        }
    }

    /**
     * Convenient byte array encryption method (auto-generates IV)
     *
     * @param data byte array to be encrypted
     * @param key key byte array
     * @return result object containing IV and cipher text
     */
    public static EncryptBytesResult encrypt(byte[] data, byte[] key) {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        byte[] cipherText = encrypt(data, key, iv);
        return new EncryptBytesResult(cipherText, iv);
    }

    /**
     * Convenient byte array encryption method (auto-generates IV) using string key
     *
     * @param data byte array to be encrypted
     * @param key Base64 encoded key
     * @return result object containing IV and cipher text
     */
    public static EncryptBytesResult encrypt(byte[] data, String key) {
        String iv = generateIV();
        String cipherText = null;
        return new EncryptBytesResult(encrypt(data, key, iv), Base64.getDecoder().decode(iv));
    }

    /**
     * Encryption result wrapper class
     */
    public static class EncryptResult {
        private final String cipherText;
        private final String iv;

        public EncryptResult(String cipherText, String iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }

        public String getCipherText() {
            return cipherText;
        }

        public String getIv() {
            return iv;
        }

        @Override
        public String toString() {
            return "EncryptResult{" +
                    "cipherText='" + cipherText + '\'' +
                    ", iv='" + iv + '\'' +
                    '}';
        }
    }

    /**
     * Byte array encryption result wrapper class
     */
    public static class EncryptBytesResult {
        private final byte[] cipherText;
        private final byte[] iv;

        public EncryptBytesResult(byte[] cipherText, byte[] iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }

        public byte[] getCipherText() {
            return cipherText;
        }

        public byte[] getIv() {
            return iv;
        }

        @Override
        public String toString() {
            return "EncryptBytesResult{" +
                    "cipherText=" + Base64.getEncoder().encodeToString(cipherText) +
                    ", iv=" + Base64.getEncoder().encodeToString(iv) +
                    '}';
        }
    }
}

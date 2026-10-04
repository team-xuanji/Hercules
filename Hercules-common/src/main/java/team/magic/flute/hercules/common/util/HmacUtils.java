package team.magic.flute.hercules.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public final class HmacUtils {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HmacUtils() {}

    /** Compute HMAC-SHA256 and output lowercase hexadecimal. secret is the executor identity ID, and message is the canonicalized operation string. */
    public static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return toHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }

    /** Constant-time comparison to prevent timing side-channel byte-by-byte probing of the MAC. */
    public static boolean verify(String secret, String message, String expectedSign) {
        if (expectedSign == null) {
            return false;
        }
        String actual = hmacSha256Hex(secret, message);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedSign.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }
}

package Modulo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

    // Public static methods for different hash types
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    public static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    public static String md5(String input) {
        return hash(input, "MD5");
    }

    // Core hash function
    public static String hash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported hash algorithm: " + algorithm, e);
        }
    }

    // Convert byte array to hex string
    private static String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String hexChar = Integer.toHexString(0xff & b);
            if (hexChar.length() == 1) hex.append('0');
            hex.append(hexChar);
        }
        return hex.toString();
    }
}

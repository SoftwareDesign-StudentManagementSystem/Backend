package com.iEdu.global.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AESUtil {
    private static final String CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String ECB_ALGORITHM = "AES"; // ECB by default
    private static final int IV_LENGTH = 16;
    private static final byte[] KEY = "MySuperSecretKey".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");

            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    // 기존 ECB 방식 복호화 (마이그레이션 용)
    public static String legacyDecrypt(String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance(ECB_ALGORITHM); // defaults to ECB
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            byte[] decrypted = cipher.doFinal(decoded);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Legacy AES decryption failed", e);
        }
    }

    // CBC 실패 시 ECB fallback
    public static String safeDecrypt(String encryptedValue) {
        try {
            return decrypt(encryptedValue); // CBC
        } catch (Exception e) {
            try {
                return legacyDecrypt(encryptedValue); // fallback to ECB
            } catch (Exception ex) {
                throw new RuntimeException("Decryption failed (CBC and legacy both failed)", ex);
            }
        }
    }
}

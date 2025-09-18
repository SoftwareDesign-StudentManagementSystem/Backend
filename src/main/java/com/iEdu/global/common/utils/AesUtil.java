package com.iEdu.global.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AesUtil {
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12;       // 12 bytes 권장 (96비트)

    // 키는 반드시 16, 24, 32바이트 중 하나
    private static final byte[] KEY = "MySuperSecretKey".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            // 랜덤 IV 생성
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            // 결과 = IV + 암호문(+태그 포함)
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            // IV 분리
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            // 암호문 + 태그 분리
            byte[] encrypted = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }
}

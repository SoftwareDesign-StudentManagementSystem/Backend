package com.iEdu.global.common.utils.Aes;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AesUtil {
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12;       // 12 bytes 권장 (96비트)
    private static final byte[] KEY;               // 키는 반드시 16, 24, 32바이트 중 하나

    static {
        // 1순위: 환경변수 AES_KEY (예: openssl rand -base64 32 결과)
        String config = System.getenv("AES_KEY");
        // 2순위: JVM 시스템 프로퍼티 (-Daes.key=...) 도 허용
        if (config == null || config.isBlank()) {
            config = System.getProperty("aes.key");
        }
        if (config == null || config.isBlank()) {
            throw new IllegalStateException(
                    "AES 키가 설정되지 않았습니다. 환경변수 AES_KEY 또는 시스템 프로퍼티 aes.key 를 설정하세요."
            );
        }
        byte[] keyBytes;
        try {
            // 보통 Base64로 관리하니까 우선 Base64 디코딩 시도
            keyBytes = Base64.getDecoder().decode(config);
        } catch (IllegalArgumentException e) {
            // Base64가 아니면 그냥 문자열 바이트 그대로 사용 (백업 플랜)
            keyBytes = config.getBytes(StandardCharsets.UTF_8);
        }
        int len = keyBytes.length;
        if (len != 16 && len != 24 && len != 32) {
            throw new IllegalStateException(
                    "AES 키 길이는 16/24/32바이트만 허용됩니다. (현재: " + len + "바이트)"
            );
        }
        KEY = keyBytes;
    }

    private AesUtil() {}    // 유틸 클래스이므로 인스턴스화 방지

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

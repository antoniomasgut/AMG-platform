package com.amg.digitalitzacio.vault.application;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class VaultEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    @Value("${VAULT_MASTER_KEY:#{null}}")
    private String masterKeyBase64;

    @PostConstruct
    void init() {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            byte[] key = new byte[32];
            secureRandom.nextBytes(key);
            secretKey = new SecretKeySpec(key, "AES");
            return;
        }
        byte[] decoded = Base64.getDecoder().decode(masterKeyBase64);
        if (decoded.length != 32) {
            throw new IllegalStateException("VAULT_MASTER_KEY must decode to exactly 32 bytes (256 bits)");
        }
        secretKey = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String rawValue) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

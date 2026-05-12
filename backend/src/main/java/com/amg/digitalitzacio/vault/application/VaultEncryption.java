package com.amg.digitalitzacio.vault.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class VaultEncryption {

    private final TextEncryptor encryptor;

    public VaultEncryption(@Value("${VAULT_MASTER_KEY:deadbeef-dead-beef-dead-beefdeadbeef}") String masterKey) {
        this.encryptor = Encryptors.text(masterKey, "deadbeefdeadbeef");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        return encryptor.decrypt(encryptedText);
    }

    public String mask(String value) {
        if (value == null || value.length() < 4) return value;
        return "***" + value.substring(value.length() - 4);
    }
}

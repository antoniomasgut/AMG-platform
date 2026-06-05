package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class VaultEncryptionTest {

    @Autowired private VaultEncryption vaultEncryption;

    @Test
    void encryptAndDecrypt_roundtrip_preservesValue() {
        var original = "my-secret-api-key-12345";
        var encrypted = vaultEncryption.encrypt(original);
        var decrypted = vaultEncryption.decrypt(encrypted);

        assertThat(encrypted, not(equalTo(original)));
        assertThat(decrypted, is(original));
    }

    @Test
    void encrypt_producesDifferentCiphertext_eachTime() {
        var e1 = vaultEncryption.encrypt("constant-value");
        var e2 = vaultEncryption.encrypt("constant-value");

        assertThat(e1, not(equalTo(e2)));
    }

    @Test
    void decrypt_returnsOriginalValue() {
        var encrypted = vaultEncryption.encrypt("smtp-password-123");
        var decrypted = vaultEncryption.decrypt(encrypted);

        assertThat(decrypted, is("smtp-password-123"));
    }

    @Test
    void encrypt_handlesSpecialCharacters() {
        var value = "contraseña & spécial chars: ñ á é € @ # $ % ^ & * ( )";
        var encrypted = vaultEncryption.encrypt(value);
        var decrypted = vaultEncryption.decrypt(encrypted);

        assertThat(decrypted, is(value));
    }

    @Test
    void encrypt_andDecrypt_workForLongValues() {
        var value = "A".repeat(1000);
        var encrypted = vaultEncryption.encrypt(value);
        var decrypted = vaultEncryption.decrypt(encrypted);

        assertThat(decrypted, is(value));
    }

    @Test
    void decrypt_invalidCiphertext_throwsException() {
        assertThrows(RuntimeException.class,
                () -> vaultEncryption.decrypt("invalid-ciphertext"));
    }
}

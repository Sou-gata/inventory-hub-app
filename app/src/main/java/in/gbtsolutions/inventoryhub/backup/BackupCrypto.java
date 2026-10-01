package in.gbtsolutions.inventoryhub.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import in.gbtsolutions.inventoryhub.Configurations;

public class BackupCrypto {

    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 12;
    public static final int PBKDF2_ITERATIONS = 65536;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int GCM_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static class EncryptedPayload {
        public final byte[] salt;
        public final byte[] iv;
        public final byte[] ciphertext;

        public EncryptedPayload(@NonNull byte[] salt, @NonNull byte[] iv, @NonNull byte[] ciphertext) {
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }

    @NonNull
    public static String resolvePassword(@Nullable String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return Configurations.DEFAULT_BACKUP_PASSWORD;
        }
        return userInput.trim();
    }

    @NonNull
    public static EncryptedPayload encrypt(@NonNull byte[] plaintext, @NonNull String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] iv = new byte[IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        SecretKey secretKey = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);
        return new EncryptedPayload(salt, iv, ciphertext);
    }

    @NonNull
    public static byte[] decrypt(@NonNull byte[] ciphertext, @NonNull String password, @NonNull byte[] salt, @NonNull byte[] iv) throws Exception {
        SecretKey secretKey = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    @NonNull
    private static SecretKey deriveKey(@NonNull String password, @NonNull byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}

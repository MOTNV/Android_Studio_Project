package com.kunsan.anonletters.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CryptoManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "AnonCounselRSAKey";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12; // 12 bytes recommended for GCM

    private static CryptoManager instance;
    private final KeyStore keyStore;

    private CryptoManager() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Keystore", e);
        }
    }

    public static synchronized CryptoManager getInstance() {
        if (instance == null) {
            instance = new CryptoManager();
        }
        return instance;
    }

    // 1. RSA-2048 Key Pair Generation & Keystore Storage
    public PublicKey getOrCreateRSAKeyPair() throws Exception {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
            
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .build();

            keyPairGenerator.initialize(spec);
            return keyPairGenerator.generateKeyPair().getPublic();
        } else {
            return keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        }
    }

    public PrivateKey getRSAPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
    }

    // Helper: Generate AES Key
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    // 2. AES-256-GCM Encryption
    // Returns Base64 encoded string containing IV + EncryptedData
    public String encryptMessage(String plainText, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combine IV and Encrypted Data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    // 2. AES-256-GCM Decryption
    public String decryptMessage(String encryptedBase64, SecretKey aesKey) throws Exception {
        byte[] combined = Base64.decode(encryptedBase64, Base64.DEFAULT);

        // Extract IV
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

        // Extract Encrypted Data
        int encryptedSize = combined.length - IV_LENGTH;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedSize);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // 3. Encrypt AES Key with RSA Public Key (Hybrid Encryption)
    public String encryptAESKey(SecretKey aesKey, PublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedKeyBytes = cipher.doFinal(aesKey.getEncoded());
        return Base64.encodeToString(encryptedKeyBytes, Base64.DEFAULT);
    }
    
    // Helper: Decrypt AES Key with RSA Private Key
    public SecretKey decryptAESKey(String encryptedAESKeyBase64, PrivateKey rsaPrivateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] encryptedBytes = Base64.decode(encryptedAESKeyBase64, Base64.DEFAULT);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedBytes);
        return new javax.crypto.spec.SecretKeySpec(decryptedKeyBytes, KeyProperties.KEY_ALGORITHM_AES);
    }
}

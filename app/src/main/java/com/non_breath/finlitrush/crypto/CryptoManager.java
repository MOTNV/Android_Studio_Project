package com.non_breath.finlitrush.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.non_breath.finlitrush.model.EncryptionResult;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
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
    private static final String RSA_ALIAS = "anoncounsel_rsa_key";
    private static final int IV_SIZE = 12;
    private final KeyStore keyStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoManager(android.content.Context context) {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            ensureRsaKey();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize keystore", e);
        }
    }

    private void ensureRsaKey() throws Exception {
        if (keyStore.containsAlias(RSA_ALIAS)) {
            return;
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
        );
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setUserAuthenticationRequired(false)
                .build();
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    private SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public EncryptionResult encrypt(String plainText) {
        try {
            SecretKey aesKey = generateAesKey();
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] cipherBytes = aesCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            PublicKey publicKey = keyStore.getCertificate(RSA_ALIAS).getPublicKey();
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.WRAP_MODE, publicKey);
            byte[] wrappedKey = rsaCipher.wrap(aesKey);

            return new EncryptionResult(
                    Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP),
                    Base64.encodeToString(wrappedKey, Base64.NO_WRAP)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public String decrypt(EncryptionResult encrypted) {
        if (encrypted == null || !encrypted.isValid()) {
            return "";
        }
        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSA_ALIAS, null);
            PrivateKey privateKey = entry.getPrivateKey();

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.UNWRAP_MODE, privateKey);
            SecretKey aesKey = (SecretKey) rsaCipher.unwrap(
                    Base64.decode(encrypted.getEncryptedAesKey(), Base64.NO_WRAP),
                    KeyProperties.KEY_ALGORITHM_AES,
                    Cipher.SECRET_KEY
            );

            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            aesCipher.init(
                    Cipher.DECRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(128, Base64.decode(encrypted.getIv(), Base64.NO_WRAP))
            );
            byte[] plain = aesCipher.doFinal(Base64.decode(encrypted.getCipherText(), Base64.NO_WRAP));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}

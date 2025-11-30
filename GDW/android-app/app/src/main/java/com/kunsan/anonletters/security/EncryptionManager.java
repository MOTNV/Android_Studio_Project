package com.kunsan.anonletters.security;

import android.util.Base64;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {

    private static final String ALGORITHM = "AES";
    // In a real E2EE app, this key would be negotiated via Diffie-Hellman or similar.
    // For this demo, we use a hardcoded key or a generated one.
    private static final byte[] TEST_KEY = "MySuperSecretKey".getBytes(); 

    public static String encrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedByteValue = cipher.doFinal(value.getBytes("utf-8"));
        return Base64.encodeToString(encryptedByteValue, Base64.DEFAULT);
    }

    public static String decrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedValue64 = Base64.decode(value, Base64.DEFAULT);
        byte[] decryptedByteValue = cipher.doFinal(decryptedValue64);
        return new String(decryptedByteValue, "utf-8");
    }

    private static Key generateKey() throws Exception {
        return new SecretKeySpec(TEST_KEY, ALGORITHM);
    }
}

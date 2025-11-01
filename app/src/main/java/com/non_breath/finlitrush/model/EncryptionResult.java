package com.non_breath.finlitrush.model;

public class EncryptionResult {
    private final String cipherText;
    private final String iv;
    private final String encryptedAesKey;

    public EncryptionResult(String cipherText, String iv, String encryptedAesKey) {
        this.cipherText = cipherText;
        this.iv = iv;
        this.encryptedAesKey = encryptedAesKey;
    }

    public String getCipherText() {
        return cipherText;
    }

    public String getIv() {
        return iv;
    }

    public String getEncryptedAesKey() {
        return encryptedAesKey;
    }

    public boolean isValid() {
        return cipherText != null && iv != null && encryptedAesKey != null;
    }
}

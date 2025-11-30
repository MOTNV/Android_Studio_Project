package com.kunsan.anonletters.data;

import com.google.firebase.Timestamp;

public class Message {
    private String text;
    private String senderId;
    private Timestamp timestamp;

    private String recipientId;
    private String encryptedAESKey;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String text, String senderId, String recipientId, String encryptedAESKey, Timestamp timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.encryptedAESKey = encryptedAESKey;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getEncryptedAESKey() {
        return encryptedAESKey;
    }

    public void setEncryptedAESKey(String encryptedAESKey) {
        this.encryptedAESKey = encryptedAESKey;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

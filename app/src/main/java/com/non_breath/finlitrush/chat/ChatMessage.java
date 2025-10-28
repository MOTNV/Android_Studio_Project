package com.non_breath.finlitrush.chat;

public class ChatMessage {
    public String text;
    public String senderId;
    public String senderName;
    public long createdAt;
    public String recipientName;
    public String recipientEmail;

    // Firestore needs no-arg constructor
    public ChatMessage() {
    }

    public ChatMessage(String text, String senderId, String senderName, String recipientName, String recipientEmail, long createdAt) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.createdAt = createdAt;
    }
}

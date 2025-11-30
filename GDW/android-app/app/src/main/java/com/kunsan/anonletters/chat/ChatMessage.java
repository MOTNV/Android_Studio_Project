package com.kunsan.anonletters.chat;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private final String text;
    private final boolean isSentByMe;
    private final Timestamp timestamp;

    public ChatMessage(String text, boolean isSentByMe, Timestamp timestamp) {
        this.text = text;
        this.isSentByMe = isSentByMe;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}

package com.non_breath.finlitrush.model;

import java.util.List;

public class Message {
    private final String messageId;
    private final String body;
    private final String senderAlias;
    private final String recipient;
    private final String category;
    private final String priority;
    private final List<String> keywords;
    private final long createdAt;
    private final long expiresAt;
    private final String status;
    private final boolean outbound;

    public Message(String messageId,
                   String body,
                   String senderAlias,
                   String recipient,
                   String category,
                   String priority,
                   List<String> keywords,
                   long createdAt,
                   long expiresAt,
                   String status,
                   boolean outbound) {
        this.messageId = messageId;
        this.body = body;
        this.senderAlias = senderAlias;
        this.recipient = recipient;
        this.category = category;
        this.priority = priority;
        this.keywords = keywords;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.outbound = outbound;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getBody() {
        return body;
    }

    public String getSenderAlias() {
        return senderAlias;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOutbound() {
        return outbound;
    }
}

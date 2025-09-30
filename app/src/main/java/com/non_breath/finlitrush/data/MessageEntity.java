package com.non_breath.finlitrush.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String messageId;

    @NonNull
    public String cipherText;

    @NonNull
    public String iv;

    @NonNull
    public String encryptedAesKey;

    public String senderAlias;
    public String recipient;
    public String category;
    public String priority;
    public String keywords;
    public long createdAt;
    public long expiresAt;
    public String status;
    public boolean outbound;

    public MessageEntity(@NonNull String messageId,
                         @NonNull String cipherText,
                         @NonNull String iv,
                         @NonNull String encryptedAesKey,
                         String senderAlias,
                         String recipient,
                         String category,
                         String priority,
                         String keywords,
                         long createdAt,
                         long expiresAt,
                         String status,
                         boolean outbound) {
        this.messageId = messageId;
        this.cipherText = cipherText;
        this.iv = iv;
        this.encryptedAesKey = encryptedAesKey;
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
}

package com.kunsan.anonletters.data.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    // 보안 목적: 메시지의 고유 식별자입니다. 순차적 ID 대신 UUID를 사용하여 추측 공격을 방지합니다.
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    private String messageId;

    // 보안 목적: 메시지 내용은 평문으로 저장되지 않고 AES로 암호화되어 저장됩니다.
    // 기기 분실이나 DB 덤프 시에도 내용을 보호하기 위함입니다.
    @ColumnInfo(name = "encrypted_content")
    private String encryptedContent;

    // 보안 목적: 정확한 타임스탬프 대신 5분 단위로 라운딩된 시간을 저장합니다.
    // 이는 타이밍 공격(Timing Attack)이나 메타데이터 분석을 통한 사용자 식별을 어렵게 합니다.
    @ColumnInfo(name = "timestamp")
    private Long timestamp;

    // 보안 목적: 발신자가 본인인지 여부만 저장합니다.
    @ColumnInfo(name = "is_sent_by_me")
    private boolean isSentByMe;

    // 보안 목적: 실제 사용자 ID 대신 해싱된 값을 저장하여 익명성을 보장합니다.
    // 동일한 사용자가 보낸 메시지임은 식별 가능하지만, 실제 사용자가 누구인지는 알 수 없게 합니다.
    @ColumnInfo(name = "sender_hash")
    private String senderHash;

    public MessageEntity(@NonNull String messageId, String encryptedContent, Long timestamp, boolean isSentByMe, String senderHash) {
        this.messageId = messageId;
        this.encryptedContent = encryptedContent;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
        this.senderHash = senderHash;
    }

    @NonNull
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public void setEncryptedContent(String encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }

    public void setSentByMe(boolean sentByMe) {
        isSentByMe = sentByMe;
    }

    public String getSenderHash() {
        return senderHash;
    }

    public void setSenderHash(String senderHash) {
        this.senderHash = senderHash;
    }
}

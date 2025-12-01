package com.non_breath.finlitrush.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.non_breath.finlitrush.crypto.CryptoManager;
import com.non_breath.finlitrush.data.MessageDao;
import com.non_breath.finlitrush.data.MessageEntity;
import com.non_breath.finlitrush.firebase.RemoteMessageService;
import com.non_breath.finlitrush.llm.LlmAnalyzer;
import com.non_breath.finlitrush.model.AnalysisResult;
import com.non_breath.finlitrush.model.EncryptionResult;
import com.non_breath.finlitrush.model.Message;
import com.non_breath.finlitrush.network.RemoteLlmClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MessageRepository {

    private static final long THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30);
    private final MessageDao messageDao;
    private final CryptoManager cryptoManager;
    private final LlmAnalyzer llmAnalyzer;
    private final RemoteMessageService remoteMessageService;
    private final RemoteLlmClient remoteLlmClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageRepository(MessageDao messageDao,
                             CryptoManager cryptoManager,
                             LlmAnalyzer llmAnalyzer,
                             RemoteMessageService remoteMessageService,
                             RemoteLlmClient remoteLlmClient) {
        this.messageDao = messageDao;
        this.cryptoManager = cryptoManager;
        this.llmAnalyzer = llmAnalyzer;
        this.remoteMessageService = remoteMessageService;
        this.remoteLlmClient = remoteLlmClient;
    }

    public LiveData<List<Message>> observeMessages() {
        MediatorLiveData<List<Message>> liveData = new MediatorLiveData<>();
        liveData.addSource(messageDao.observeAll(), entities -> executor.execute(() -> {
            List<Message> mapped = new ArrayList<>();
            for (MessageEntity entity : entities) {
                mapped.add(mapEntity(entity));
            }
            liveData.postValue(mapped);
        }));
        return liveData;
    }

    public void sendMessage(String plainText, AnalysisResult providedAnalysis, String senderAlias) {
        executor.execute(() -> {
            AnalysisResult analysis = providedAnalysis != null ? providedAnalysis : llmAnalyzer.analyze(plainText);
            EncryptionResult encrypted = cryptoManager.encrypt(plainText);
            if (encrypted == null || !encrypted.isValid()) {
                return;
            }
            List<String> keywordItems = analysis != null ? analysis.getKeywords() : new ArrayList<>();
            String keywords = String.join(", ", keywordItems);
            long now = System.currentTimeMillis();
            long expiresAt = now + THIRTY_DAYS_MS;
            MessageEntity entity = new MessageEntity(
                    UUID.randomUUID().toString(),
                    encrypted.getCipherText(),
                    encrypted.getIv(),
                    encrypted.getEncryptedAesKey(),
                    senderAlias,
                    analysis != null ? analysis.getRecommendedRecipient() : "미지정",
                    analysis != null ? analysis.getCategory() : "일반 문의",
                    analysis != null ? analysis.getPriority() : "일반",
                    keywords,
                    now,
                    expiresAt,
                    "LOCAL_ONLY",
                    true
            );
            messageDao.insert(entity);
            if (remoteMessageService != null) {
                remoteMessageService.push(entity);
            }
        });
    }

    public AnalysisResult analyzeDraft(String text) {
        if (remoteLlmClient != null) {
            AnalysisResult remote = remoteLlmClient.analyze(text);
            if (remote != null) return remote;
        }
        return llmAnalyzer.analyze(text);
    }

    private Message mapEntity(MessageEntity entity) {
        EncryptionResult encrypted = new EncryptionResult(entity.cipherText, entity.iv, entity.encryptedAesKey);
        String plain = cryptoManager.decrypt(encrypted);
        List<String> keywordList = new ArrayList<>();
        if (entity.keywords != null && !entity.keywords.isEmpty()) {
            keywordList = new ArrayList<>(Arrays.asList(entity.keywords.split(",\\s*")));
        }
        return new Message(
                entity.messageId,
                plain,
                entity.senderAlias,
                entity.recipient,
                entity.category,
                entity.priority,
                keywordList,
                entity.createdAt,
                entity.expiresAt,
                entity.status,
                entity.outbound
        );
    }
}

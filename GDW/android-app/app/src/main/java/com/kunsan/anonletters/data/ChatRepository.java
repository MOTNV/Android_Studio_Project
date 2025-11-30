package com.kunsan.anonletters.data;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.kunsan.anonletters.ai.GeminiAnalyzer;
import com.kunsan.anonletters.data.room.AppDatabase;
import com.kunsan.anonletters.data.room.MessageDao;
import com.kunsan.anonletters.data.room.MessageEntity;
import com.kunsan.anonletters.security.CryptoManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.SecretKey;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.security.PublicKey;

public class ChatRepository {

    private static final String TAG = "ChatRepository";
    private final MessageDao messageDao;
    private final CollectionReference messagesRef;
    private final GeminiAnalyzer geminiAnalyzer;
    private final CryptoManager cryptoManager;
    private final ExecutorService executor;
    private final String currentUserId;
    
    private ListenerRegistration firestoreListener;

    public ChatRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        messageDao = db.messageDao();
        
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        messagesRef = firestore.collection("messages");
        
        geminiAnalyzer = new GeminiAnalyzer();
        cryptoManager = CryptoManager.getInstance();
        executor = Executors.newSingleThreadExecutor();
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
                        
        startSync();
    }

    // 3. Firestore 실시간 업데이트를 Room DB에 동기화
    private void startSync() {
        firestoreListener = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshots != null) {
                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                // Firestore에서 받은 메시지를 Room에 저장
                                // (이미 암호화된 상태라고 가정하거나, 필요시 처리)
                                saveToLocal(message);
                            }
                        }
                    });
                }
            });
    }

    private void saveToLocal(Message message) {
        // Firestore 메시지를 Room Entity로 변환하여 저장
        // 주의: 로컬에서 보낸 메시지는 이미 저장되었을 수 있으므로 중복 처리 필요 (OnConflictStrategy.REPLACE 사용 중)
        try {
            String senderHash = String.valueOf(message.getSenderId().hashCode());
            boolean isSentByMe = message.getSenderId().equals(currentUserId);
            
            // timestamp 처리
            long time = message.getTimestamp() != null ? message.getTimestamp().toDate().getTime() : System.currentTimeMillis();
            long roundedTimestamp = (time / 300000) * 300000;

            MessageEntity entity = new MessageEntity(
                UUID.randomUUID().toString(), // 실제로는 Firestore ID를 사용하는 것이 좋음
                message.getText(), // 여기서는 Firestore에 저장된 텍스트(암호화된)를 그대로 저장
                roundedTimestamp,
                isSentByMe,
                senderHash
            );
            messageDao.insertMessage(entity);
        } catch (Exception e) {
            Log.e(TAG, "Error saving to local DB", e);
        }
    }

    public LiveData<List<MessageEntity>> getAllMessages() {
        return messageDao.getAllMessages();
    }

    public Query getMessagesQuery() {
        return messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);
    }

    // 2. 메시지 전송 로직: Gemini -> 암호화 -> Firestore -> Room
    // 2. 메시지 전송 로직: Gemini -> 암호화 -> Firestore -> Room
    // Refactored: Split into analyze and send steps

    public void analyzeMessage(String text, MutableLiveData<AnalysisResult> result) {
        geminiAnalyzer.analyzeText(text, new GeminiAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(JSONObject jsonResult) {
                try {
                    String category = jsonResult.optString("category", "일반");
                    String sentiment = jsonResult.optString("sentiment", "일반");
                    JSONArray recipientsJson = jsonResult.optJSONArray("recipient");
                    List<String> recipients = new ArrayList<>();
                    if (recipientsJson != null) {
                        for (int i = 0; i < recipientsJson.length(); i++) {
                            recipients.add(recipientsJson.getString(i));
                        }
                    } else {
                        // Fallback if single string
                        recipients.add(jsonResult.optString("recipient", "상담사"));
                    }
                    
                    AnalysisResult analysis = new AnalysisResult(category, sentiment, recipients);
                    result.postValue(analysis);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing analysis result", e);
                    // Post error or empty result? For now, just log.
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini Analysis Failed", t);
                // Handle failure
            }
        });
    }

    public interface SendCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void sendConsultationRequest(String text, String recipientId, SendCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Encryption
                SecretKey aesKey = cryptoManager.generateAESKey();
                String encryptedContent = cryptoManager.encryptMessage(text, aesKey);

                // 2. Encrypt AES Key with Recipient's Public Key
                // TODO: Fetch actual recipient's public key. Using own public key for demo/testing purposes.
                PublicKey recipientPublicKey = cryptoManager.getOrCreateRSAKeyPair(); 
                String encryptedAESKey = cryptoManager.encryptAESKey(aesKey, recipientPublicKey);

                // 3. Send to Firestore (Encrypted)
                Message message = new Message(
                    encryptedContent, 
                    currentUserId, 
                    recipientId,
                    encryptedAESKey,
                    Timestamp.now()
                );
                
                messagesRef.add(message)
                    .addOnSuccessListener(documentReference -> {
                        // 4. Save to Room (Encrypted)
                        saveToLocal(message);
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e);
                    });

            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                if (callback != null) callback.onFailure(e);
            }
        });
    }



    public void onCleared() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}

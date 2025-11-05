package com.non_breath.finlitrush.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.non_breath.finlitrush.data.MessageEntity;

import java.util.HashMap;
import java.util.Map;

public class RemoteMessageService {

    private static final String TAG = "RemoteMessageService";
    private final FirebaseProvider provider;

    public RemoteMessageService(FirebaseProvider provider) {
        this.provider = provider;
    }

    public void push(MessageEntity entity) {
        if (provider == null || !provider.isReady()) {
            return;
        }
        FirebaseFirestore db = provider.getFirestore();
        if (db == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", entity.messageId);
        payload.put("cipherText", entity.cipherText);
        payload.put("iv", entity.iv);
        payload.put("encryptedAesKey", entity.encryptedAesKey);
        payload.put("senderAlias", entity.senderAlias);
        payload.put("recipient", entity.recipient);
        payload.put("category", entity.category);
        payload.put("priority", entity.priority);
        payload.put("keywords", entity.keywords);
        payload.put("createdAt", entity.createdAt);
        payload.put("expiresAt", entity.expiresAt);
        payload.put("status", entity.status);
        payload.put("outbound", entity.outbound);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid != null) {
            payload.put("uid", uid);
        }

        db.collection("messages")
                .document(entity.messageId)
                .set(payload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Message uploaded: " + entity.messageId))
                .addOnFailureListener(e -> Log.w(TAG, "Upload failed: " + e.getMessage()));
    }
}

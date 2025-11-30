package com.kunsan.anonletters.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.kunsan.anonletters.data.room.AppDatabase;
import com.kunsan.anonletters.data.room.MessageDao;
import com.kunsan.anonletters.data.room.MessageEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageRepository {

    private final CollectionReference messagesRef;
    private final MessageDao messageDao;
    private final ExecutorService executor;

    public MessageRepository(Application application) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        messagesRef = firestore.collection("messages");
        AppDatabase db = AppDatabase.getDatabase(application);
        messageDao = db.messageDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void sendMessage(Message message, MessageEntity messageEntity) {
        // 1. Save to Firestore
        messagesRef.add(message);
        
        // 2. Save to Room (Local Encrypted)
        executor.execute(() -> messageDao.insertMessage(messageEntity));
    }

    public Query getFirestoreMessagesQuery() {
        return messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public LiveData<List<MessageEntity>> getLocalMessages() {
        return messageDao.getAllMessages();
    }
}

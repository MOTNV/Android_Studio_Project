package com.kunsan.anonletters.chat;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.kunsan.anonletters.data.ChatRepository;
import com.kunsan.anonletters.data.room.MessageEntity;
import com.kunsan.anonletters.data.AnalysisResult;
import com.google.firebase.firestore.Query;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<AnalysisResult> analysisResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSending = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final LiveData<List<MessageEntity>> allMessages;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        allMessages = repository.getAllMessages();
    }

    public LiveData<AnalysisResult> getAnalysisResult() {
        return analysisResult;
    }

    public LiveData<Boolean> getIsSending() {
        return isSending;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<List<MessageEntity>> getAllMessages() {
        return allMessages;
    }

    public Query getMessagesQuery() {
        return repository.getMessagesQuery();
    }

    public void analyze(String text) {
        if (text.trim().isEmpty()) return;
        isSending.setValue(true);
        repository.analyzeMessage(text, analysisResult);
        isSending.setValue(false); // Analysis is async but repository posts value. 
        // Ideally repository should handle loading state or callback. 
        // For simplicity, we assume analysis returns quickly or we handle loading in UI via observation.
    }

    public void send(String text, String recipientId) {
        if (text.trim().isEmpty() || recipientId == null) return;
        
        isSending.setValue(true);
        repository.sendConsultationRequest(text, recipientId, new ChatRepository.SendCallback() {
            @Override
            public void onSuccess() {
                isSending.postValue(false);
                // Navigate or show success
            }

            @Override
            public void onFailure(Exception e) {
                isSending.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.onCleared();
    }
    
    public String getCurrentUserId() {
        // Repository doesn't expose this directly, but we can get it from FirebaseAuth if needed
        // or add a method to Repository. For now, keeping it simple.
        return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
               com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
    }
}

package com.non_breath.finlitrush.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.non_breath.finlitrush.crypto.CryptoManager;
import com.non_breath.finlitrush.data.AppDatabase;
import com.non_breath.finlitrush.firebase.FirebaseProvider;
import com.non_breath.finlitrush.firebase.RemoteMessageService;
import com.non_breath.finlitrush.llm.LlmAnalyzer;
import com.non_breath.finlitrush.llm.RecipientDirectory;
import com.non_breath.finlitrush.model.AnalysisResult;
import com.non_breath.finlitrush.model.IdentityProvider;
import com.non_breath.finlitrush.model.Message;
import com.non_breath.finlitrush.network.RemoteLlmClient;
import com.non_breath.finlitrush.repository.MessageRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageViewModel extends AndroidViewModel {

    private final MessageRepository repository;
    private final MutableLiveData<AnalysisResult> analysis = new MutableLiveData<>(AnalysisResult.empty());
    private final LiveData<List<Message>> messages;
    private final MutableLiveData<String> anonId = new MutableLiveData<>();
    private final IdentityProvider identityProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        RecipientDirectory recipientDirectory = new RecipientDirectory();
        LlmAnalyzer analyzer = new LlmAnalyzer(recipientDirectory);
        CryptoManager cryptoManager = new CryptoManager(application);
        FirebaseProvider firebaseProvider = new FirebaseProvider(application);
        RemoteMessageService remoteMessageService = new RemoteMessageService(firebaseProvider);
        // Base URL: emulator -> 10.0.2.2:8000 , device -> LAN IP:8000
        String baseUrl = "http://10.0.2.2:8000/";
        RemoteLlmClient remoteLlmClient = new RemoteLlmClient(baseUrl);
        this.repository = new MessageRepository(db.messageDao(), cryptoManager, analyzer, remoteMessageService, remoteLlmClient);
        this.identityProvider = new IdentityProvider(application);
        this.messages = repository.observeMessages();
        this.anonId.setValue(identityProvider.getOrCreateAnonId());
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<AnalysisResult> getAnalysis() {
        return analysis;
    }

    public LiveData<String> getAnonId() {
        return anonId;
    }

    public void refreshAnonId() {
        anonId.postValue(identityProvider.getOrCreateAnonId());
    }

    public void analyzeDraft(String text) {
        executor.execute(() -> analysis.postValue(repository.analyzeDraft(text)));
    }

    public void sendMessage(String text) {
        AnalysisResult current = analysis.getValue();
        executor.execute(() -> repository.sendMessage(text, current, identityProvider.getOrCreateAnonId()));
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
}

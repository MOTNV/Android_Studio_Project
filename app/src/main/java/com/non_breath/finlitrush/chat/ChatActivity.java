package com.non_breath.finlitrush.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.non_breath.finlitrush.databinding.ActivityChatBinding;
import com.non_breath.finlitrush.model.IdentityProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPIENT_NAME = "recipient_name";
    public static final String EXTRA_RECIPIENT_EMAIL = "recipient_email";
    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_PEER_ID = "peer_id";

    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private FirebaseFirestore firestore;
    private String chatId;
    private String meId;
    private String recipientName;
    private String recipientEmail;
    private String peerId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recipientName = getIntent().getStringExtra(EXTRA_RECIPIENT_NAME);
        recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        if (TextUtils.isEmpty(recipientName)) recipientName = "교수";
        if (TextUtils.isEmpty(recipientEmail)) recipientEmail = "unknown@example.com";

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(recipientName);

        if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            meId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        } else {
            meId = new IdentityProvider(this).getOrCreateAnonId();
        }
        adapter = new ChatAdapter(meId);
        binding.chatList.setLayoutManager(new LinearLayoutManager(this));
        binding.chatList.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        String overrideChatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        if (!TextUtils.isEmpty(overrideChatId)) {
            chatId = overrideChatId;
        } else {
            chatId = buildChatId(meId, recipientEmail);
        }

        bindListeners();
        observeMessages();
    }

    private void bindListeners() {
        binding.sendButton.setOnClickListener(v -> {
            String text = binding.messageInput.getText() != null ? binding.messageInput.getText().toString().trim() : "";
            if (text.isEmpty()) return;
            sendMessage(text);
            binding.messageInput.setText("");
        });
    }

    private void observeMessages() {
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    List<ChatMessage> current = new ArrayList<>();
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                        current.add(msg);
                    }
                    adapter.submit(current);
                    binding.chatList.scrollToPosition(Math.max(0, current.size() - 1));
                    binding.emptyState.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void sendMessage(String text) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : meId;
        ChatMessage msg = new ChatMessage(text, uid, meId, recipientName, recipientEmail, System.currentTimeMillis());
        Map<String, Object> meta = new HashMap<>();
        String peer = peerId != null ? peerId : recipientEmail;
        // 참여자: 내 식별자, peer, 인증 UID가 있으면 함께 추가
        Set<String> participants = new HashSet<>();
        participants.add(meId);
        participants.add(peer);
        if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getUid() != null) {
            participants.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        meta.put("participants", new ArrayList<>(participants));
        meta.put("recipientEmail", recipientEmail);
        meta.put("recipientName", recipientName);
        meta.put("lastMessage", text);
        meta.put("lastAt", msg.createdAt);
        firestore.collection("chats")
                .document(chatId)
                .set(meta, SetOptions.merge());
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(msg);
    }

    private String buildChatId(String a, String b) {
        String raw = a + "|" + b;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(raw.getBytes());
            byte[] digest = md.digest();
            Formatter fmt = new Formatter();
            for (byte by : digest) fmt.format(Locale.US, "%02x", by);
            return fmt.toString();
        } catch (NoSuchAlgorithmException e) {
            return raw.replace("@", "_").replace(".", "_");
        }
    }
}

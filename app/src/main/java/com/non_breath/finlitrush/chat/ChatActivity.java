package com.non_breath.finlitrush.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.non_breath.finlitrush.databinding.ActivityChatBinding;
import com.non_breath.finlitrush.model.IdentityProvider;
import com.non_breath.finlitrush.auth.RoleManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    private RoleManager roleManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recipientName = getIntent().getStringExtra(EXTRA_RECIPIENT_NAME);
        recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        if (TextUtils.isEmpty(recipientEmail)) recipientEmail = "unknown@example.com";

        // recipientName is already formatted by ChatListActivity (e.g., "익명 1", "정동원 교수님")
        // Only format if it's truly empty or generic
        if (TextUtils.isEmpty(recipientName)) {
            // If recipientName is not provided, try to extract from email
            if (recipientEmail.contains("@") && !recipientEmail.startsWith("anon-")) {
                String localPart = recipientEmail.split("@")[0];
                recipientName = localPart + " 교수님";
            } else if (peerId != null && peerId.startsWith("anon-")) {
                // Anonymous peer - will be resolved with role-based display later
                recipientName = "상담 대상";
            } else {
                recipientName = "대화 상대";
            }
        }

        android.util.Log.d("ChatActivity", "Intent extras - recipientName: " + recipientName + ", recipientEmail: " + recipientEmail + ", peerId: " + peerId);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(recipientName);

        roleManager = new RoleManager(this);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // 로그인된 경우 UID로 구분
            meId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

        // Refresh role from Firebase before sending messages
        roleManager.refreshRole(role -> {
            android.util.Log.d("ChatActivity", "Role refreshed: " + role);
            adapter.setRole(role);
            adapter.notifyDataSetChanged(); // Refresh display with new role
        });
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
                    // 항상 전체 스냅샷을 다시 그려 새 메시지로 인한 누락을 방지
                    for (DocumentChange ignored : snapshots.getDocumentChanges()) {
                        // no-op; we will iterate all docs below
                    }
                    snapshots.getDocuments().forEach(doc -> current.add(doc.toObject(ChatMessage.class)));
                    adapter.submit(current);
                    binding.chatList.scrollToPosition(Math.max(0, current.size() - 1));
                    binding.emptyState.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void sendMessage(String text) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        RoleManager.Role myRole = roleManager.getRole();

        // 학생(STUDENT)은 항상 익명 ID만 사용, 교수/관리자는 이메일 사용
        String senderId = meId;
        String senderName = meId;

        if (myRole == RoleManager.Role.RESPONDENT || myRole == RoleManager.Role.ADMIN) {
            // 교수/관리자: 이메일 정보 사용 가능
            if (user != null && !TextUtils.isEmpty(user.getEmail())) {
                senderName = user.getEmail();
            }
        } else {
            // 학생: 항상 익명 ID 사용
            IdentityProvider idProvider = new IdentityProvider(this);
            String anonId = idProvider.getOrCreateAnonId();
            senderId = anonId;
            senderName = anonId;
        }

        android.util.Log.d("ChatActivity", "Sending message - myRole: " + myRole + ", senderId: " + senderId + ", senderName: " + senderName);

        ChatMessage msg = new ChatMessage(text, senderId, senderName, recipientName, recipientEmail, System.currentTimeMillis());
        Map<String, Object> meta = new HashMap<>();
        String peer = peerId != null ? peerId : recipientEmail;

        // 참여자 식별: 역할에 따라 다르게 처리
        Set<String> participants = new HashSet<>();

        if (myRole == RoleManager.Role.STUDENT) {
            // 학생: 익명 ID만 추가
            IdentityProvider idProvider = new IdentityProvider(this);
            participants.add(idProvider.getOrCreateAnonId());
        } else {
            // 교수/관리자: UID, 이메일 모두 추가
            if (user != null) {
                if (!TextUtils.isEmpty(user.getUid())) {
                    participants.add(user.getUid());
                }
                if (!TextUtils.isEmpty(user.getEmail())) {
                    participants.add(user.getEmail());
                }
            }
        }

        // 상대방 ID 추가
        participants.add(peer);

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

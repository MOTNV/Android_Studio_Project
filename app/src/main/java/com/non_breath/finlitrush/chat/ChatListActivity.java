package com.non_breath.finlitrush.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.non_breath.finlitrush.databinding.ActivityChatListBinding;
import com.non_breath.finlitrush.model.IdentityProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private ActivityChatListBinding binding;
    private ChatListAdapter adapter;
    private FirebaseFirestore firestore;
    private String myKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setTitle("채팅방");

        firestore = FirebaseFirestore.getInstance();
        // 내 식별 키: 이메일 우선, 없으면 uid, 그것도 없으면 로컬 anonId
        if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            myKey = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            myKey = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            myKey = new IdentityProvider(this).getOrCreateAnonId();
        }

        adapter = new ChatListAdapter(item -> {
            // peerId: 참가자 중 내 식별자가 아닌 값
            String peerId = item.peerId != null ? item.peerId : "익명";
            android.content.Intent intent = new android.content.Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CHAT_ID, item.chatId);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_NAME, peerId);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_EMAIL, peerId); // 채팅 상대 식별자로 사용
            intent.putExtra(ChatActivity.EXTRA_PEER_ID, peerId);
            startActivity(intent);
        });
        binding.chatList.setLayoutManager(new LinearLayoutManager(this));
        binding.chatList.setAdapter(adapter);
        binding.chatList.setHasFixedSize(true);

        loadChats();
    }

    private void loadChats() {
        binding.progress.setVisibility(View.VISIBLE);
        firestore.collection("chats")
                .whereArrayContains("participants", myKey)
                .orderBy("lastAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    binding.progress.setVisibility(View.GONE);
                    if (snap == null) return;
                    List<ChatSummary> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatSummary cs = map(doc);
                        if (cs != null) list.add(cs);
                    }
                    adapter.submit(list);
                    binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private ChatSummary map(DocumentSnapshot doc) {
        List<String> participants = (List<String>) doc.get("participants");
        String peerId = null;
        if (participants != null) {
            for (String p : participants) {
                if (p != null && !p.equals(myKey)) {
                    peerId = p;
                    break;
                }
            }
        }
        String lastMsg = doc.getString("lastMessage");
        Long lastAt = doc.getLong("lastAt");
        if (lastAt == null) lastAt = 0L;
        return new ChatSummary(doc.getId(), peerId != null ? peerId : "익명", lastMsg != null ? lastMsg : "", lastAt);
    }

    public static class ChatSummary {
        public final String chatId;
        public final String peerId;
        public final String lastMessage;
        public final long lastAt;

        public ChatSummary(String chatId, String peerId, String lastMessage, long lastAt) {
            this.chatId = chatId;
            this.peerId = peerId;
            this.lastMessage = lastMessage;
            this.lastAt = lastAt;
        }
    }
}

package com.non_breath.finlitrush.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.non_breath.finlitrush.R;
import com.non_breath.finlitrush.auth.RoleManager;
import com.non_breath.finlitrush.databinding.ActivityChatListBinding;
import com.non_breath.finlitrush.model.IdentityProvider;
import com.non_breath.finlitrush.settings.SettingsActivity;
import com.non_breath.finlitrush.data.ProfessorDirectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatListActivity extends AppCompatActivity {

    private ActivityChatListBinding binding;
    private ChatListAdapter adapter;
    private FirebaseFirestore firestore;
    private final List<String> myKeys = new ArrayList<>();
    private RoleManager roleManager;
    private final Map<String, Integer> anonNumberMap = new HashMap<>();
    private int nextAnonNumber = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("채팅 목록");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        roleManager = new RoleManager(this);
        firestore = FirebaseFirestore.getInstance();
        buildIdentityKeys();

        adapter = new ChatListAdapter(item -> {
            String peerId = item.peerId != null ? item.peerId : "익명";
            android.content.Intent intent = new android.content.Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CHAT_ID, item.chatId);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_NAME, item.recipientName != null ? item.recipientName : item.displayName);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_EMAIL, item.recipientEmail != null ? item.recipientEmail : peerId);
            intent.putExtra(ChatActivity.EXTRA_PEER_ID, peerId);
            startActivity(intent);
        });
        binding.chatList.setLayoutManager(new LinearLayoutManager(this));
        binding.chatList.setAdapter(adapter);
        binding.chatList.setHasFixedSize(true);

        loadChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        roleManager.refreshRole(role -> runOnUiThread(() -> {
            buildIdentityKeys();
            loadChats();
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buildIdentityKeys() {
        myKeys.clear();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (!TextUtils.isEmpty(user.getUid())) myKeys.add(user.getUid());
            if (!TextUtils.isEmpty(user.getEmail())) myKeys.add(user.getEmail());
        } else {
            String anon = new IdentityProvider(this).getOrCreateAnonId();
            if (!TextUtils.isEmpty(anon)) myKeys.add(anon);
        }
        if (myKeys.isEmpty()) {
            Toast.makeText(this, "사용자 식별자가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadChats() {
        if (myKeys.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        firestore.collection("chats")
                .whereArrayContainsAny("participants", myKeys)
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
        String recipientName = doc.getString("recipientName");
        String recipientEmail = doc.getString("recipientEmail");

        String peerId = null;
        String studentAnonId = null;
        String professorEmailCandidate = null;
        if (participants != null) {
            Set<String> myKeySet = new HashSet<>(myKeys);
            for (String p : participants) {
                if (p == null) continue;
                if (p.startsWith("anon-")) {
                    studentAnonId = p;
                }
                if (!myKeySet.contains(p)) {
                    peerId = p;
                    if (isEmail(p) && !p.startsWith("anon-")) {
                        professorEmailCandidate = p;
                    }
                }
            }
        }

        // 우선순위: recipientEmail -> peerId -> participants에서 발견한 이메일
        if (professorEmailCandidate == null && isEmail(recipientEmail) && !isAnonymousLabel(recipientEmail)) {
            professorEmailCandidate = recipientEmail;
        }
        if (professorEmailCandidate == null && isEmail(peerId) && !isAnonymousLabel(peerId)) {
            professorEmailCandidate = peerId;
        }

        RoleManager.Role myRole = roleManager.getRole();
        String displayName;
        if (myRole == RoleManager.Role.RESPONDENT || myRole == RoleManager.Role.ADMIN) {
            String anonKey = !TextUtils.isEmpty(studentAnonId)
                    ? studentAnonId
                    : (peerId != null && peerId.startsWith("anon-") ? peerId : null);
            if (anonKey != null) {
                int n = anonNumberMap.computeIfAbsent(anonKey, k -> nextAnonNumber++);
                displayName = "익명 " + n;
            } else {
                displayName = "익명";
            }
        } else {
            // 학생: 교수 이름을 최대한 복구
            if (!isAnonymousLabel(recipientName)) {
                displayName = ensureProfessorSuffix(recipientName);
            } else if (professorEmailCandidate != null) {
                displayName = professorNameFromEmail(professorEmailCandidate);
            } else {
                displayName = "익명";
            }
        }

        String lastMsg = doc.getString("lastMessage");
        Long lastAt = doc.getLong("lastAt");
        if (lastAt == null) lastAt = 0L;
        return new ChatSummary(
                doc.getId(),
                peerId != null ? peerId : "익명",
                displayName,
                recipientName,
                recipientEmail,
                lastMsg != null ? lastMsg : "",
                lastAt
        );
    }

    private boolean isAnonymousLabel(String value) {
        if (value == null) return true;
        return value.startsWith("anon-") || value.startsWith("익명");
    }

    private String nameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return localPart;
    }

    private boolean isEmail(String v) {
        return v != null && v.contains("@");
    }

    private String professorNameFromEmail(String email) {
        if (TextUtils.isEmpty(email)) return "익명";
        for (ProfessorDirectory.Professor p : ProfessorDirectory.getAll()) {
            if (email.equalsIgnoreCase(p.email)) {
                return ensureProfessorSuffix(p.name);
            }
        }
        // 못 찾으면 로컬 파트에 교수님 접미사
        return ensureProfessorSuffix(nameFromEmail(email));
    }

    private String ensureProfessorSuffix(String name) {
        if (TextUtils.isEmpty(name)) return "익명";
        String trimmed = name.trim();
        if (trimmed.contains("교수")) return trimmed;
        return trimmed + " 교수님";
    }

    public static class ChatSummary {
        public final String chatId;
        public final String peerId;
        public final String displayName;
        public final String recipientName;
        public final String recipientEmail;
        public final String lastMessage;
        public final long lastAt;

        public ChatSummary(String chatId, String peerId, String displayName, String recipientName, String recipientEmail, String lastMessage, long lastAt) {
            this.chatId = chatId;
            this.peerId = peerId;
            this.displayName = displayName;
            this.recipientName = recipientName;
            this.recipientEmail = recipientEmail;
            this.lastMessage = lastMessage;
            this.lastAt = lastAt;
        }
    }
}

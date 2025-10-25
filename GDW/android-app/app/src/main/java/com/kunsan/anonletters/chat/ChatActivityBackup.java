package com.kunsan.anonletters.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.kunsan.anonletters.R;
import com.kunsan.anonletters.data.Message;

import android.widget.Toast;
import com.kunsan.anonletters.chat.ChatMessage;
import com.kunsan.anonletters.security.CryptoManager;
import androidx.appcompat.widget.Toolbar;
import java.security.PrivateKey;
import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class ChatActivityBackup extends AppCompatActivity {

    private static final String TAG = "ChatActivityBackup";
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("상담 채팅");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        adapter = new ChatAdapter(viewModel.getCurrentUserId());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editTextMessage.getText().toString();
                if (!text.isEmpty()) {
                    // Direct send to the current recipient (passed via Intent or default)
                    // For now, we default to "상담사" or get from Intent if we had logic for that.
                    // Since ChatActivity is now just a chat room, we assume the session is established.
                    // But we need a recipientId.
                    // Let's get it from Intent.
                    String recipientId = getIntent().getStringExtra("recipientId");
                    if (recipientId == null) recipientId = "상담사"; // Fallback
                    
                    viewModel.send(text, recipientId);
                    editTextMessage.setText("");
                }
            }
        });

        // Analysis result observation removed as it's handled in ConsultationRequestActivity

        // Observe Sending State
        viewModel.getIsSending().observe(this, isSending -> {
            buttonSend.setEnabled(!isSending);
            // Show/Hide progress bar if available
        });

        // Observe Error
        viewModel.getError().observe(this, errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(ChatActivityBackup.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getMessagesQuery().addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error);
                    return;
                }

                List<ChatMessage> chatMessages = new ArrayList<>();
                CryptoManager cryptoManager = CryptoManager.getInstance();
                String currentUserId = viewModel.getCurrentUserId();

                
                adapter.setMessages(chatMessages);
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }
    private void confirmLeaveChat() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("정말로 채팅방을 나가시겠습니까? 대화 내용이 삭제됩니다.")
                .setPositiveButton("나가기", (dialog, which) -> {
                    Toast.makeText(ChatActivityBackup.this, "채팅방을 나갔습니다.", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .setNegativeButton("취소", null)
                .show();
    }
    // END of Menu Implementation

    // showRecipientSelectionDialog removed
}

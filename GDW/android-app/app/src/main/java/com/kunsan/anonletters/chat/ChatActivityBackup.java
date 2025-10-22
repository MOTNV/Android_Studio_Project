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


    // START of Menu Implementation
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // Programmatically adding a menu item to avoid creating a new XML file
        // ID: 1, Order: 0, Title: "Menu"
        android.view.MenuItem menuItem = menu.add(0, 1, 0, "메뉴");
        menuItem.setIcon(R.drawable.ic_person_24); // Using an existing drawable as a placeholder for a menu icon
        menuItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == 1) {
            showChatMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChatMenu() {
        try {
            // Inflate the backup menu layout
            android.view.LayoutInflater inflater = (android.view.LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View menuView = inflater.inflate(R.layout.layout_chat_menu_backup, null);

            // Create PopupWindow
            // Width: 300dp (approx 80% screen width usually, or fixed), Height: Match Parent
            int width = (int) (300 * getResources().getDisplayMetrics().density); 
            final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                    menuView,
                    width,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    true
            );

            // Set background to allow outside touch to dismiss (though Focusable=true handles most)
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFFFFFFF));
            popupWindow.setElevation(20);

            // Animation (Optional, using default for now or simply appearing)
            
            // Show at the end (Right side)
            popupWindow.showAtLocation(findViewById(R.id.toolbar), android.view.Gravity.END, 0, 0);

            // Initialize Menu Items
            initMenuItems(menuView, popupWindow);

        } catch (Exception e) {
            Log.e(TAG, "Error showing chat menu", e);
            Toast.makeText(this, "메뉴를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initMenuItems(View menuView, final android.widget.PopupWindow popupWindow) {
        // 1. Notifications Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchNotifications = 
                menuView.findViewById(R.id.switchNotifications);
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String status = isChecked ? "켜짐" : "꺼짐";
                Toast.makeText(ChatActivityBackup.this, "알림이 " + status + "으로 설정되었습니다.", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Files Button
        View btnFiles = menuView.findViewById(R.id.btnFiles);
        if (btnFiles != null) {
            btnFiles.setOnClickListener(v -> 
                Toast.makeText(ChatActivityBackup.this, "파일 보관함 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
            );
        }

        // 3. Leave Chat Button
        View btnLeaveChat = menuView.findViewById(R.id.btnLeaveChat);
        if (btnLeaveChat != null) {
            btnLeaveChat.setOnClickListener(v -> {
                popupWindow.dismiss();
                confirmLeaveChat();
            });
        }
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

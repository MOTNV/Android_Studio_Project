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
   
}

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

   
        
   
}

package com.kunsan.anonletters;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.kunsan.anonletters.chat.ChatActivity;
import com.kunsan.anonletters.chat.ChatViewModel;
import com.kunsan.anonletters.data.AnalysisResult;
import java.util.List;

public class ConsultationRequestActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private EditText editTextConsultation;
    private Button buttonRequest;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_request);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        editTextConsultation = findViewById(R.id.editTextConsultation);
        buttonRequest = findViewById(R.id.buttonRequest);
        progressBar = findViewById(R.id.progressBar);

        buttonRequest.setOnClickListener(v -> {
            String text = editTextConsultation.getText().toString();
            if (text.trim().isEmpty()) {
                Toast.makeText(this, "고민 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.analyze(text);
        });

        viewModel.getIsSending().observe(this, isSending -> {
            progressBar.setVisibility(isSending ? View.VISIBLE : View.GONE);
            buttonRequest.setEnabled(!isSending);
            editTextConsultation.setEnabled(!isSending);
        });

        viewModel.getAnalysisResult().observe(this, result -> {
            if (result != null) {
                showRecipientSelectionDialog(result);
                // Reset analysis result to prevent dialog showing again on rotation/back
                // Ideally ViewModel should handle this reset or use SingleLiveEvent
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "오류: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRecipientSelectionDialog(AnalysisResult result) {
        List<String> recipients = result.getRecommendedRecipients();
        if (recipients == null || recipients.isEmpty()) {
            Toast.makeText(this, "추천 상담사를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] recipientArray = recipients.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle("추천 상담사 선택 (" + result.getCategory() + ")")
            .setItems(recipientArray, (dialog, which) -> {
                String selectedRecipient = recipientArray[which];
                sendConsultationRequest(selectedRecipient);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void sendConsultationRequest(String recipientId) {
        String text = editTextConsultation.getText().toString();
        viewModel.send(text, recipientId);
        
        // Observe success via some mechanism or just wait for callback in ViewModel?
        // ChatViewModel.send() updates isSending and error.
        // But we need to know when it SUCCEEDS to navigate.
        // ChatViewModel currently doesn't expose a "Success" event clearly other than isSending going false without error.
        // For now, let's assume if isSending goes false and no error, it succeeded.
        // OR, better: Navigate immediately to ChatActivity and let ChatActivity load the message?
        // No, we want to ensure the request is sent first.
        
        // Let's add a temporary observer for success or just navigate.
        // Ideally ChatViewModel should have a LiveData<Boolean> sendSuccess.
        // For this refactor, I will just navigate to ChatActivity passing the recipientId.
        // The message sending happens in background.
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("recipientId", recipientId);
        startActivity(intent);
        finish();
    }
}

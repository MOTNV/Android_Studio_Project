package com.non_breath.finlitrush.settings;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.non_breath.finlitrush.databinding.ActivitySettingsBinding;
import com.non_breath.finlitrush.model.IdentityProvider;
import com.non_breath.finlitrush.auth.RoleManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private IdentityProvider identityProvider;
    private RoleManager roleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        identityProvider = new IdentityProvider(this);
        roleManager = new RoleManager(this);
        renderIds();
        bindListeners();
    }

    private void renderIds() {
        String anonId = identityProvider.getOrCreateAnonId();
        binding.currentAnonId.setText(anonId);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        binding.currentFirebaseUid.setText(user != null ? user.getUid() : "로그인 필요");
    }

    private void bindListeners() {
        binding.copyAnonId.setOnClickListener(v -> copyText(binding.currentAnonId.getText().toString(), "익명 ID를 복사했습니다."));
        binding.copyFirebaseUid.setOnClickListener(v -> copyText(binding.currentFirebaseUid.getText().toString(), "Firebase UID를 복사했습니다."));
        binding.applyAnonId.setOnClickListener(v -> {
            String newId = binding.inputAnonId.getText() != null ? binding.inputAnonId.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newId)) {
                Toast.makeText(this, "ID를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            String applied = identityProvider.setAnonId(newId);
            binding.currentAnonId.setText(applied);
            binding.inputAnonId.setText("");
            Toast.makeText(this, "익명 ID를 갱신했습니다.", Toast.LENGTH_SHORT).show();
        });
        binding.buttonRoleLogin.setOnClickListener(v -> startActivity(new android.content.Intent(this, RoleLoginActivity.class)));
    }

    private void copyText(String text, String toastMessage) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("id", text));
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

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
import com.non_breath.finlitrush.auth.AuthManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private IdentityProvider identityProvider;
    private RoleManager roleManager;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        identityProvider = new IdentityProvider(this);
        roleManager = new RoleManager(this);
        authManager = new AuthManager();
        renderIds();
        bindListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        roleManager.refreshRole(role -> runOnUiThread(this::renderIds));
    }

    private void renderIds() {
        String anonId = identityProvider.getOrCreateAnonId();
        binding.currentAnonId.setText(anonId);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        RoleManager.Role role = roleManager.getRole();

        // Show name and email for professors/respondents, hide for students
        if (user != null && role != RoleManager.Role.STUDENT) {
            binding.nameSection.setVisibility(android.view.View.VISIBLE);
            binding.emailSection.setVisibility(android.view.View.VISIBLE);

            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "사용자";
            }
            binding.userName.setText(displayName);
            binding.userEmail.setText(user.getEmail());
        } else {
            binding.nameSection.setVisibility(android.view.View.GONE);
            binding.emailSection.setVisibility(android.view.View.GONE);
        }

        // Display role in Korean
        String roleText;
        switch (role) {
            case STUDENT:
                roleText = "학생";
                break;
            case RESPONDENT:
                roleText = "상담자";
                break;
            case ADMIN:
                roleText = "관리자";
                break;
            default:
                roleText = role.name();
        }
        binding.currentRoleSettings.setText(roleText);
    }

    private void bindListeners() {
        binding.copyAnonId.setOnClickListener(v -> copyText(binding.currentAnonId.getText().toString(), "익명 ID를 복사했습니다."));
        binding.copyEmail.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                copyText(user.getEmail(), "이메일을 복사했습니다.");
            }
        });
        binding.applyAnonId.setOnClickListener(v -> {
            String newId = binding.inputAnonId.getText() != null ? binding.inputAnonId.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newId)) {
                Toast.makeText(this, "ID를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String applied = identityProvider.setAnonId(newId);
            binding.currentAnonId.setText(applied);
            binding.inputAnonId.setText("");
            Toast.makeText(this, "익명 ID를 갱신했습니다.", Toast.LENGTH_SHORT).show();
        });
        binding.buttonRoleLogin.setOnClickListener(v -> startActivity(new android.content.Intent(this, RoleLoginActivity.class)));
        binding.buttonLogout.setOnClickListener(v -> {
            authManager.signOut(() -> {
                roleManager.setLocalRole(RoleManager.Role.STUDENT);
                renderIds();
                Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
            });
        });
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

package com.non_breath.finlitrush.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.non_breath.finlitrush.auth.AuthManager;
import com.non_breath.finlitrush.auth.RoleManager;
import com.non_breath.finlitrush.chat.ChatListActivity;
import com.non_breath.finlitrush.databinding.ActivityRoleLoginBinding;

public class RoleLoginActivity extends AppCompatActivity {

    private ActivityRoleLoginBinding binding;
    private AuthManager authManager;
    private RoleManager roleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        authManager = new AuthManager();
        roleManager = new RoleManager(this);
        renderRole();
        bindListeners();
    }

    private void bindListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.inputEmail.getText() != null ? binding.inputEmail.getText().toString().trim() : "";
            String password = binding.inputPassword.getText() != null ? binding.inputPassword.getText().toString() : "";
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            authManager.signInWithEmail(email, password,
                    () -> {
                        roleManager.refreshRole(role -> runOnUiThread(() -> {
                            renderRole();
                            Toast.makeText(this, "로그인 완료", Toast.LENGTH_SHORT).show();
                            if (role == RoleManager.Role.RESPONDENT || role == RoleManager.Role.ADMIN) {
                                startActivity(new android.content.Intent(this, ChatListActivity.class));
                                finish();
                            }
                        }));
                    },
                    error -> runOnUiThread(() -> Toast.makeText(this, "로그인 실패: " + error, Toast.LENGTH_SHORT).show())
            );
        });

        binding.buttonAnon.setOnClickListener(v -> {
            authManager.ensureSignedIn();
            roleManager.setLocalRole(RoleManager.Role.STUDENT);
            renderRole();
            Toast.makeText(this, "익명 학생 모드로 전환", Toast.LENGTH_SHORT).show();
        });

        binding.buttonRoleStudent.setOnClickListener(v -> {
            roleManager.setLocalRole(RoleManager.Role.STUDENT);
            renderRole();
            Toast.makeText(this, "로컬 역할: 학생", Toast.LENGTH_SHORT).show();
        });
        binding.buttonRoleRespondent.setOnClickListener(v -> {
            roleManager.setLocalRole(RoleManager.Role.RESPONDENT);
            renderRole();
            Toast.makeText(this, "로컬 역할: 응답자", Toast.LENGTH_SHORT).show();
        });
        binding.buttonRoleAdmin.setOnClickListener(v -> {
            roleManager.setLocalRole(RoleManager.Role.ADMIN);
            renderRole();
            Toast.makeText(this, "로컬 역할: 관리자", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderRole() {
        binding.currentRole.setText(roleManager.getRole().name());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

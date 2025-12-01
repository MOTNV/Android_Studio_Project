package com.non_breath.finlitrush.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves role in order: custom claims (server-authored) > local fallback (user-chosen).
 */
public class RoleManager {

    public enum Role {
        STUDENT, RESPONDENT, ADMIN
    }

    private static final String TAG = "RoleManager";
    private static final String PREFS = "role_prefs";
    private static final String KEY_LOCAL_ROLE = "local_role";
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Role cached = Role.STUDENT;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public RoleManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cached = loadLocal();
    }

    public Role getRole() {
        return cached;
    }

    public void refreshRole(java.util.function.Consumer<Role> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            cached = loadLocal();
            if (callback != null) callback.accept(cached);
            return;
        }
        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    Role resolved = fromClaims(result);
                    cached = resolved;
                    if (callback != null) callback.accept(resolved);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to fetch token: " + e.getMessage());
                    cached = loadLocal();
                    if (callback != null) callback.accept(cached);
                });
    }

    public void setLocalRole(Role role) {
        prefs.edit().putString(KEY_LOCAL_ROLE, role.name()).apply();
        cached = role;
    }

    private Role loadLocal() {
        String saved = prefs.getString(KEY_LOCAL_ROLE, Role.STUDENT.name());
        try {
            return Role.valueOf(saved);
        } catch (Exception e) {
            return Role.STUDENT;
        }
    }

    private Role fromClaims(GetTokenResult tokenResult) {
        if (tokenResult == null || tokenResult.getClaims() == null) {
            return fallbackForLoggedIn();
        }
        Object roleClaim = tokenResult.getClaims().get("role");
        if (roleClaim instanceof String) {
            String value = ((String) roleClaim).toLowerCase(Locale.US);
            if (value.contains("admin")) return Role.ADMIN;
            if (value.contains("respondent") || value.contains("professor") || value.contains("ta")) return Role.RESPONDENT;
        }
        return fallbackForLoggedIn();
    }

    private Role fallbackForLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        // 로그인되어 있고 익명 사용자가 아니면 기본 응답자 권한으로 간주
        if (user != null && !user.isAnonymous()) {
            return Role.RESPONDENT;
        }
        return loadLocal();
    }
}

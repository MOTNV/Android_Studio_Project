package com.non_breath.finlitrush.auth;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;

/**
 * Handles anonymous sign-in so each device gets a unique UID without showing a login UI.
 * The UID is persisted by Firebase and reset only on app uninstall/clear data.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private final FirebaseAuth auth;

    public AuthManager() {
        this.auth = FirebaseAuth.getInstance();
    }

    public void ensureSignedIn() {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            Log.d(TAG, "Already signed in (uid=" + current.getUid() + ")");
            return;
        }
        auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        Log.d(TAG, "Anonymous sign-in success (uid=" + user.getUid() + ")");
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Anonymous sign-in failed: " + e.getMessage()));
    }

    public void signInWithEmail(String email, String password, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "Email sign-in success (uid=" + result.getUser().getUid() + ")");
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Email sign-in failed: " + e.getMessage());
                    if (onError != null) onError.accept(e.getMessage());
                });
    }

    public void signOut(Runnable onComplete) {
        auth.signOut();
        if (onComplete != null) onComplete.run();
    }
}

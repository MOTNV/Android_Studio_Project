package com.non_breath.finlitrush.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public class IdentityProvider {

    private static final String PREFS = "anoncounsel_identity";
    private static final String KEY_ANON_ID = "anon_id";
    private final SharedPreferences prefs;

    public IdentityProvider(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getOrCreateAnonId() {
        String cached = prefs.getString(KEY_ANON_ID, null);
        if (cached != null) {
            return cached;
        }
        return setAnonId(generate());
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : result) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String generate() {
        String raw = UUID.randomUUID().toString();
        String hashed = hash(raw);
        return "anon-" + hashed.substring(0, Math.min(hashed.length(), 10));
    }

    public String setAnonId(String newId) {
        String candidate = newId == null ? "" : newId.trim();
        if (candidate.isEmpty()) {
            candidate = generate();
        }
        prefs.edit().putString(KEY_ANON_ID, candidate).apply();
        return candidate;
    }
}

package com.non_breath.finlitrush.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Centralizes Firebase initialization and allows graceful fallback when
 * google-services.json is missing (build-time warning already logged in Gradle).
 */
public class FirebaseProvider {

    private static final String TAG = "FirebaseProvider";
    private final boolean ready;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final FirebaseMessaging messaging;
    private final FirebaseFunctions functions;

    public FirebaseProvider(Context context) {
        boolean initialized = false;
        FirebaseFirestore fs = null;
        FirebaseAuth fa = null;
        FirebaseMessaging fm = null;
        FirebaseFunctions ff = null;
        try {
            FirebaseApp app;
            if (FirebaseApp.getApps(context).isEmpty()) {
                app = FirebaseApp.initializeApp(context);
            } else {
                app = FirebaseApp.getInstance();
            }
            if (app != null) {
                initialized = true;
                fs = FirebaseFirestore.getInstance(app);
                fa = FirebaseAuth.getInstance(app);
                fm = FirebaseMessaging.getInstance();
                ff = FirebaseFunctions.getInstance();
                Log.d(TAG, "Firebase initialized with options: " + optionsSummary(app.getOptions()));
            } else {
                Log.w(TAG, "FirebaseApp initialization returned null. Missing google-services.json?");
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization skipped: " + e.getMessage());
        }
        this.ready = initialized;
        this.firestore = fs;
        this.auth = fa;
        this.messaging = fm;
        this.functions = ff;
    }

    public boolean isReady() {
        return ready;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseMessaging getMessaging() {
        return messaging;
    }

    public FirebaseFunctions getFunctions() {
        return functions;
    }

    private String optionsSummary(FirebaseOptions options) {
        if (options == null) return "no options";
        return "appId=" + options.getApplicationId() + ", projectId=" + options.getProjectId();
    }
}

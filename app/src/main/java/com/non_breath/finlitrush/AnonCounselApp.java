package com.non_breath.finlitrush;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.non_breath.finlitrush.auth.AuthManager;
import com.non_breath.finlitrush.auth.RoleManager;
import com.non_breath.finlitrush.work.DeleteExpiredMessagesWorker;

import java.util.concurrent.TimeUnit;

public class AnonCounselApp extends Application {

    private static final String CLEANUP_WORK_NAME = "cleanup-expired-messages";
    private AuthManager authManager;
    private RoleManager roleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        authManager = new AuthManager();
        authManager.ensureSignedIn();
        roleManager = new RoleManager(this);
        roleManager.refreshRole(null);
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DeleteExpiredMessagesWorker.class,
                1, TimeUnit.DAYS
        ).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CLEANUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }
}

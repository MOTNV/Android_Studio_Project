package com.non_breath.finlitrush.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.non_breath.finlitrush.data.AppDatabase;
import com.non_breath.finlitrush.data.MessageDao;

public class DeleteExpiredMessagesWorker extends Worker {

    private static final String TAG = "DeleteExpiredWorker";

    public DeleteExpiredMessagesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            MessageDao dao = AppDatabase.getInstance(getApplicationContext()).messageDao();
            long now = System.currentTimeMillis();
            int deleted = dao.deleteExpired(now);
            Log.d(TAG, "Expired messages deleted: " + deleted);
            return Result.success();
        } catch (Exception e) {
            Log.w(TAG, "Cleanup failed: " + e.getMessage());
            return Result.retry();
        }
    }
}

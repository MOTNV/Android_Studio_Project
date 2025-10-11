package com.non_breath.finlitrush.network;

import android.util.Log;

import com.non_breath.finlitrush.model.AnalysisResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RemoteLlmClient {

    private static final String TAG = "RemoteLlmClient";
    private final LlmApiService api;

    public RemoteLlmClient(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(LlmApiService.class);
    }

    public AnalysisResult analyze(String text) {
        try {
            Response<LlmApiService.AnalysisResponse> response = api.analyze(new LlmApiService.AnalysisRequest(text)).execute();
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "LLM analyze failed: " + response.code());
                return null;
            }
            LlmApiService.AnalysisResponse body = response.body();
            List<String> recipients = new ArrayList<>();
            if (body.recipients != null) {
                for (LlmApiService.Recipient r : body.recipients) {
                    if (r != null && r.name != null && !r.name.trim().isEmpty()) {
                        recipients.add(r.name.trim());
                    }
                }
            }
            if (recipients.isEmpty() && body.recipient_name != null) {
                recipients.add(body.recipient_name);
            }
            String recommended = !recipients.isEmpty() ? recipients.get(0) : "학과조교";
            List<String> keywords = body.keywords != null ? body.keywords : Collections.emptyList();
            String category = body.category != null ? body.category : "기타";
            String priority = body.urgency != null ? body.urgency : "일반";

            Log.d(TAG, "LLM response recipients=" + recipients + " category=" + category + " urgency=" + priority + " keywords=" + keywords);
            return new AnalysisResult(category, priority, recommended, keywords, recipients);
        } catch (Exception e) {
            Log.w(TAG, "LLM analyze exception: " + e.getMessage());
            return null;
        }
    }
}

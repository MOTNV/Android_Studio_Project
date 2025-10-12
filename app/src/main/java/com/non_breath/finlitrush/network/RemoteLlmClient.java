package com.non_breath.finlitrush.network;

import android.util.Log;

import com.non_breath.finlitrush.model.AnalysisResult;

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
            Log.d(TAG, "LLM response recipient=" + body.recipient_name + " category=" + body.category + " urgency=" + body.urgency + " keywords=" + body.keywords);
            java.util.List<String> keywords = body.keywords != null ? body.keywords : java.util.Collections.emptyList();
            String category = body.category != null ? body.category : "일반 문의";
            String priority = body.urgency != null ? body.urgency : "일반";
            String recipient = body.recipient_name != null ? body.recipient_name : "학과조교";
            return new AnalysisResult(category, priority, recipient, keywords);
        } catch (Exception e) {
            Log.w(TAG, "LLM analyze exception: " + e.getMessage());
            return null;
        }
    }
}

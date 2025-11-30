package com.kunsan.anonletters.ai;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class GeminiAnalyzer {

    // NOTE: In a real app, store API KEY securely (e.g., BuildConfig or EncryptedSharedPreferences)
    private static final String API_KEY = "YOUR_API_KEY";
    private final GenerativeModelFutures model;
    private final Executor executor;

    public interface AnalysisCallback {
        void onSuccess(JSONObject result);
        void onFailure(Throwable t);
    }

    public GeminiAnalyzer() {
        // Initialize the Gemini Pro model
        GenerativeModel gm = new GenerativeModel("gemini-pro", API_KEY);
        this.model = GenerativeModelFutures.from(gm);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void analyzeText(String userText, AnalysisCallback callback) {
        if ("YOUR_API_KEY".equals(API_KEY)) {
            // Dummy mode for testing flow without API Key
            try {
                JSONObject dummy = new JSONObject();
                dummy.put("category", "테스트(API키 없음)");
                dummy.put("sentiment", "일반");
                dummy.put("recipient", "테스트상담사"); // Single string fallback logic in Repo handles this, or array
                // Repo expects array or string. Let's put array to be safe if Repo logic is strict, 
                // but Repo has fallback. Let's use simple string for now as per my plan.
                // Wait, Repo logic: optJSONArray -> if null -> optString.
                // So string is fine.
                
                // Simulate network delay
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    callback.onSuccess(dummy);
                }, 1000);
                return;
            } catch (Exception e) {
                callback.onFailure(e);
                return;
            }
        }

        String prompt = buildPrompt(userText);
        Content content = new Content.Builder().addText(prompt).build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String outputText = result.getText();
                    // Clean up markdown code blocks if present
                    if (outputText.contains("```json")) {
                        outputText = outputText.replace("```json", "").replace("```", "");
                    }
                    JSONObject jsonObject = new JSONObject(outputText);
                    callback.onSuccess(jsonObject);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, executor);
    }

    private String buildPrompt(String text) {
        return "Analyze the following text and return a JSON object with the following fields:\n" +
                "1. category: One of ['수강문의', '성적', '학업상담', '개인고민', '긴급신고']\n" +
                "2. sentiment: One of ['일반', '불만', '긴급']\n" +
                "3. recipient: One of ['담당교수', '학과조교', '학생처']\n\n" +
                "Text: \"" + text + "\"\n\n" +
                "Output JSON:";
    }
}

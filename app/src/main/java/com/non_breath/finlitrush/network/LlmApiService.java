package com.non_breath.finlitrush.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface LlmApiService {

    class AnalysisRequest {
        public String content;

        public AnalysisRequest(String content) {
            this.content = content;
        }
    }

    class Recipient {
        public String name;
        public String reason;
        public String email;
        public String office;
    }

    class AnalysisResponse {
        public String category;
        public String urgency;
        public String recipient_name;
        public String recipient_email;
        public String recipient_office;
        public List<String> keywords;
        public String reason;
        public List<Recipient> recipients;
        public String routing_source;
    }

    @Headers("Content-Type: application/json")
    @POST("/analyze")
    Call<AnalysisResponse> analyze(@Body AnalysisRequest request);
}

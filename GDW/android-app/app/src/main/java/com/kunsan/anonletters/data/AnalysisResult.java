package com.kunsan.anonletters.data;

import java.util.List;

public class AnalysisResult {
    private String category;
    private String sentiment;
    private List<String> recommendedRecipients;

    public AnalysisResult(String category, String sentiment, List<String> recommendedRecipients) {
        this.category = category;
        this.sentiment = sentiment;
        this.recommendedRecipients = recommendedRecipients;
    }

    public String getCategory() {
        return category;
    }

    public String getSentiment() {
        return sentiment;
    }

    public List<String> getRecommendedRecipients() {
        return recommendedRecipients;
    }
}

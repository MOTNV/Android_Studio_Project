package com.non_breath.finlitrush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalysisResult {
    private final String category;
    private final String priority;
    private final String recommendedRecipient;
    private final List<String> keywords;
    private final List<String> recipients;

    public AnalysisResult(String category, String priority, String recommendedRecipient, List<String> keywords) {
        this(category, priority, recommendedRecipient, keywords,
                recommendedRecipient != null ? Collections.singletonList(recommendedRecipient) : Collections.emptyList());
    }

    public AnalysisResult(String category, String priority, String recommendedRecipient, List<String> keywords, List<String> recipients) {
        this.category = category;
        this.priority = priority;
        this.recommendedRecipient = recommendedRecipient;
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.recipients = recipients != null ? new ArrayList<>(recipients) : new ArrayList<>();
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public String getRecommendedRecipient() {
        return recommendedRecipient;
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    public List<String> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }

    public static AnalysisResult empty() {
        return new AnalysisResult("분석 없음", "일반", "추천 수신자 없음", Collections.emptyList(), Collections.emptyList());
    }
}

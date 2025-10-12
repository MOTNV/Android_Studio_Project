package com.non_breath.finlitrush.llm;

import com.non_breath.finlitrush.model.AnalysisResult;
import com.non_breath.finlitrush.model.Recipient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight local analyzer used when remote LLM is unavailable.
 */
public class LlmAnalyzer {

    private final RecipientDirectory recipientDirectory;

    public LlmAnalyzer(RecipientDirectory recipientDirectory) {
        this.recipientDirectory = recipientDirectory;
    }

    public RecipientDirectory getRecipientDirectory() {
        return recipientDirectory;
    }

    public AnalysisResult analyze(String text) {
        if (text == null || text.trim().isEmpty()) {
            Recipient recipient = recipientDirectory.getRecipients().get(0);
            return new AnalysisResult("기타 문의", "일반", recipient.getName(), new ArrayList<>());
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> keywords = new ArrayList<>();
        String category = "일반 문의";
        String priority = "일반";

        List<String> academic = Arrays.asList("학점", "grade", "출석", "시험", "평가");
        List<String> assignment = Arrays.asList("과제", "숙제", "프로젝트", "실습");
        List<String> counseling = Arrays.asList("우울", "상담", "개인", "고민", "stress", "스트레스");
        List<String> emergency = Arrays.asList("폭력", "위협", "신고", "긴급", "help");
        List<String> admin = Arrays.asList("등록금", "휴학", "복학", "성적", "행정");
        List<String> blockchain = Arrays.asList("블록체인", "blockchain", "코인", "암호화폐");
        List<String> iot = Arrays.asList("iot", "사물인터넷", "센서");
        List<String> nlp = Arrays.asList("nlp", "자연어", "텍스트");
        List<String> vision = Arrays.asList("비전", "vision", "영상처리", "이미지");
        List<String> graphics = Arrays.asList("그래픽스", "graphics", "ar", "vr", "게임");

        addIfPresent(normalized, academic, keywords);
        addIfPresent(normalized, assignment, keywords);
        addIfPresent(normalized, counseling, keywords);
        addIfPresent(normalized, emergency, keywords);
        addIfPresent(normalized, admin, keywords);
        addIfPresent(normalized, blockchain, keywords);
        addIfPresent(normalized, iot, keywords);
        addIfPresent(normalized, nlp, keywords);
        addIfPresent(normalized, vision, keywords);
        addIfPresent(normalized, graphics, keywords);

        if (containsAny(normalized, emergency)) {
            category = "긴급 신고";
            priority = "긴급";
        } else if (containsAny(normalized, academic)) {
            category = "학점/시험";
            priority = keywords.contains("불만") ? "높음" : "보통";
        } else if (containsAny(normalized, assignment)) {
            category = "수업/과제";
            priority = "보통";
        } else if (containsAny(normalized, counseling)) {
            category = "상담/복지";
            priority = "높음";
        } else if (containsAny(normalized, admin)) {
            category = "행정/신청";
            priority = "보통";
        } else if (containsAny(normalized, blockchain) || containsAny(normalized, iot) || containsAny(normalized, nlp) || containsAny(normalized, vision) || containsAny(normalized, graphics)) {
            category = "학업상담";
            priority = "보통";
        }

        Recipient recipient = recipientDirectory.findBestMatch(keywords);
        return new AnalysisResult(category, priority, recipient.getName(), keywords);
    }

    private void addIfPresent(String text, List<String> targets, List<String> keywords) {
        for (String target : targets) {
            if (text.contains(target.toLowerCase(Locale.ROOT)) && !keywords.contains(target)) {
                keywords.add(target);
            }
        }
    }

    private boolean containsAny(String text, List<String> targets) {
        for (String target : targets) {
            if (text.contains(target.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}

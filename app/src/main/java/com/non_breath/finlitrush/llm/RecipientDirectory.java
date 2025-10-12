package com.non_breath.finlitrush.llm;

import com.non_breath.finlitrush.model.Recipient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecipientDirectory {

    private final List<Recipient> recipients = new ArrayList<>();

    public RecipientDirectory() {
        recipients.add(new Recipient("professor", "담당 교수", "지도교수", Arrays.asList("학점", "시험", "출석", "성적")));
        recipients.add(new Recipient("ta", "과목 조교", "조교", Arrays.asList("과제", "숙제", "과목", "실습", "프로젝트")));
        recipients.add(new Recipient("counsel", "학생 상담센터", "상담실", Arrays.asList("상담", "우울", "개인", "고민", "스트레스")));
        recipients.add(new Recipient("admin", "학과조교", "행정 담당", Arrays.asList("행정", "등록금", "휴학", "복학", "신고")));
        recipients.add(new Recipient("safety", "신고 담당", "신고 담당", Arrays.asList("폭력", "위협", "신고", "긴급", "협박")));
    }

    public Recipient findBestMatch(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return recipients.get(0);
        }
        return recipients.stream()
                .max(Comparator.comparingInt(r -> matchScore(r, keywords)))
                .orElse(recipients.get(0));
    }

    private int matchScore(Recipient recipient, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            for (String specialty : recipient.getSpecialties()) {
                if (keyword.contains(specialty) || specialty.contains(keyword)) {
                    score += 2;
                }
            }
        }
        return score;
    }

    public List<Recipient> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }
}

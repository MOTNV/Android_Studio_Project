package com.non_breath.finlitrush.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static seed data for professors. Also used as a local fallback when Firestore data is unavailable.
 */
public class ProfessorDirectory {

    public static class Professor {
        public final String name;
        public final String englishName;
        public final String title;
        public final List<String> researchAreas;
        public final String lab;
        public final String email;
        public final String phone;
        public final String location;

        public Professor(String name,
                         String englishName,
                         String title,
                         List<String> researchAreas,
                         String lab,
                         String email,
                         String phone,
                         String location) {
            this.name = name;
            this.englishName = englishName;
            this.title = title;
            this.researchAreas = researchAreas;
            this.lab = lab;
            this.email = email;
            this.phone = phone;
            this.location = location;
        }
    }

    private static final List<Professor> PROFESSORS = Collections.unmodifiableList(Arrays.asList(
            new Professor(
                    "정동원",
                    "Dongwon Jeong",
                    "교수",
                    Arrays.asList("데이터베이스", "데이터표준화", "엣지컴퓨팅", "database", "edge computing"),
                    "Information Sciences & Technology Lab",
                    "djeong@kunsan.ac.kr",
                    "(063) 469-8912",
                    "디지털정보관 151-106"
            ),
            new Professor(
                    "온병원",
                    "Byung-Won On",
                    "교수",
                    Arrays.asList("데이터 마이닝", "빅데이터", "인공지능", "강화학습", "data mining", "ai", "ml", "reinforcement learning"),
                    "Data Intelligence Lab",
                    "bwon@kunsan.ac.kr",
                    "(063) 469-8913",
                    "디지털정보관 151-109"
            ),
            new Professor(
                    "이석훈",
                    "Sukhoon Lee",
                    "교수",
                    Arrays.asList("사물인터넷", "데이터 공학", "시맨틱 웹", "헬스케어", "iot", "data engineering", "semantic web", "healthcare"),
                    "Data Semantics Lab",
                    "leha82@kunsan.ac.kr",
                    "(063) 469-8914",
                    "디지털정보관 151-108"
            ),
            new Professor(
                    "손창환",
                    "Chang-Hwan Son",
                    "교수",
                    Arrays.asList("컴퓨터 비전", "영상처리", "딥러닝", "기계학습", "그래픽스", "computer vision", "image processing", "deep learning", "machine learning", "graphics"),
                    "Computer Vision & Machine Learning Lab",
                    "cson@kunsan.ac.kr",
                    "(063) 469-8915",
                    "디지털정보관 151-105"
            ),
            new Professor(
                    "김장원",
                    "Jangwon Gim",
                    "교수",
                    Arrays.asList("실시간 빅데이터", "자연어처리", "지식그래프", "데이터 거버넌스", "real-time big data", "nlp", "knowledge graph", "data governance"),
                    "Ambient Human & Machine Intelligence Lab (AMI Lab)",
                    "jwgim@kunsan.ac.kr",
                    "(063) 469-8916",
                    "자연과학대학 4502"
            ),
            new Professor(
                    "정현준",
                    "Hyunjun Jung",
                    "교수",
                    Arrays.asList("IoT", "블록체인", "blockchain"),
                    "Blockchain Intelligence Lab",
                    "junghj85@kunsan.ac.kr",
                    "(063) 469-8917",
                    "디지털정보관 151-228"
            ),
            new Professor(
                    "김능회",
                    "Neunghoe Kim",
                    "교수",
                    Arrays.asList("소프트웨어공학", "오피니언 마이닝", "빅데이터", "software engineering", "opinion mining", "big data"),
                    "User and Information Lab",
                    "nunghoi@kunsan.ac.kr",
                    "(063) 469-8918",
                    "디지털정보관 151-340"
            ),
            new Professor(
                    "남영주",
                    "Youngju Nam",
                    "교수",
                    Arrays.asList("차량 네트워크", "IoT", "인공지능", "최적화", "vehicle network", "ai", "optimization"),
                    "Mobility Network Optimization Lab",
                    "imnyj@kunsan.ac.kr",
                    "(063) 469-8919",
                    "자연과학대학 4501"
            ),
            new Professor(
                    "마준",
                    "Jun Ma",
                    "교수",
                    Arrays.asList("그래픽스", "디지털트윈", "게임", "AR", "VR", "의료 AI", "graphics", "digital twin", "game", "ar", "vr", "medical ai"),
                    "Computer Graphics Lab",
                    "junma@kunsan.ac.kr",
                    "",
                    "디지털정보관 151-118"
            )
    ));

    public static List<Professor> getAll() {
        return new ArrayList<>(PROFESSORS);
    }
}

import firebase_admin
from firebase_admin import credentials, firestore

# 서비스 계정 키 파일 경로 (프로젝트 루트에 serviceAccount.json을 두었다고 가정)
SERVICE_KEY = "serviceAccount.json"

PROFESSORS = [
    {
        "name": "정동원",
        "englishName": "Dongwon Jeong",
        "title": "교수",
        "researchAreas": ["데이터베이스", "데이터표준화", "엣지컴퓨팅", "database", "edge computing"],
        "lab": "Information Sciences & Technology Lab",
        "email": "djeong@kunsan.ac.kr",
        "phone": "(063) 469-8912",
        "location": "디지털정보관 151-106",
    },
    {
        "name": "온병원",
        "englishName": "Byung-Won On",
        "title": "교수",
        "researchAreas": ["데이터 마이닝", "빅데이터", "인공지능", "강화학습", "data mining", "ai", "ml", "reinforcement learning"],
        "lab": "Data Intelligence Lab",
        "email": "bwon@kunsan.ac.kr",
        "phone": "(063) 469-8913",
        "location": "디지털정보관 151-109",
    },
    {
        "name": "이석훈",
        "englishName": "Sukhoon Lee",
        "title": "교수",
        "researchAreas": ["사물인터넷", "데이터 공학", "시맨틱 웹", "헬스케어", "iot", "data engineering", "semantic web", "healthcare"],
        "lab": "Data Semantics Lab",
        "email": "leha82@kunsan.ac.kr",
        "phone": "(063) 469-8914",
        "location": "디지털정보관 151-108",
    },
    {
        "name": "손창환",
        "englishName": "Chang-Hwan Son",
        "title": "교수",
        "researchAreas": ["컴퓨터 비전", "영상처리", "딥러닝", "기계학습", "그래픽스", "computer vision", "image processing", "deep learning", "machine learning", "graphics"],
        "lab": "Computer Vision & Machine Learning Lab",
        "email": "cson@kunsan.ac.kr",
        "phone": "(063) 469-8915",
        "location": "디지털정보관 151-105",
    },
    {
        "name": "김장원",
        "englishName": "Jangwon Gim",
        "title": "교수",
        "researchAreas": ["실시간 빅데이터", "자연어처리", "지식그래프", "데이터 거버넌스", "real-time big data", "nlp", "knowledge graph", "data governance"],
        "lab": "Ambient Human & Machine Intelligence Lab (AMI Lab)",
        "email": "jwgim@kunsan.ac.kr",
        "phone": "(063) 469-8916",
        "location": "자연과학대학 4502",
    },
    {
        "name": "정현준",
        "englishName": "Hyunjun Jung",
        "title": "교수",
        "researchAreas": ["IoT", "블록체인", "blockchain"],
        "lab": "Blockchain Intelligence Lab",
        "email": "junghj85@kunsan.ac.kr",
        "phone": "(063) 469-8917",
        "location": "디지털정보관 151-228",
    },
    {
        "name": "김능회",
        "englishName": "Neunghoe Kim",
        "title": "교수",
        "researchAreas": ["소프트웨어공학", "오피니언 마이닝", "빅데이터", "software engineering", "opinion mining", "big data"],
        "lab": "User and Information Lab",
        "email": "nunghoi@kunsan.ac.kr",
        "phone": "(063) 469-8918",
        "location": "디지털정보관 151-340",
    },
    {
        "name": "남영주",
        "englishName": "Youngju Nam",
        "title": "교수",
        "researchAreas": ["차량 네트워크", "IoT", "인공지능", "최적화", "vehicle network", "ai", "optimization"],
        "lab": "Mobility Network Optimization Lab",
        "email": "imnyj@kunsan.ac.kr",
        "phone": "(063) 469-8919",
        "location": "자연과학대학 4501",
    },
    {
        "name": "마준",
        "englishName": "Jun Ma",
        "title": "교수",
        "researchAreas": ["그래픽스", "디지털트윈", "게임", "AR", "VR", "의료 AI", "graphics", "digital twin", "game", "ar", "vr", "medical ai"],
        "lab": "Computer Graphics Lab",
        "email": "junma@kunsan.ac.kr",
        "phone": "",
        "location": "디지털정보관 151-118",
    },
]


def main() -> None:
    cred = credentials.Certificate(SERVICE_KEY)
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    for prof in PROFESSORS:
        doc_id = prof["email"]
        db.collection("professors").document(doc_id).set(prof)
        print(f"uploaded: {prof['name']} ({doc_id})")


if __name__ == "__main__":
    main()

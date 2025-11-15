import hashlib
import time

import firebase_admin
from firebase_admin import credentials, firestore

SERVICE_KEY = "serviceAccount.json"

# 샘플 채팅 시드 데이터
SAMPLE_CHATS = [
    {
        "anonId": "anon-demo-001",
        "recipientEmail": "djeong@kunsan.ac.kr",
        "recipientName": "정동원",
        "messages": [
            {"text": "안녕하세요, 데이터베이스 관련 상담을 받고 싶어요.", "senderId": "anon-demo-001", "senderName": "anon-demo-001"},
            {"text": "네, 구체적으로 어떤 내용인가요?", "senderId": "prof-djeong", "senderName": "정동원 교수"},
        ],
    },
    {
        "anonId": "anon-demo-002",
        "recipientEmail": "junghj85@kunsan.ac.kr",
        "recipientName": "정현준",
        "messages": [
            {"text": "블록체인 연구 주제 상담 가능합니다.", "senderId": "anon-demo-002", "senderName": "anon-demo-002"},
        ],
    },
]


def build_chat_id(anon_id: str, recipient_email: str) -> str:
    raw = f"{anon_id}|{recipient_email}"
    return hashlib.sha256(raw.encode()).hexdigest()


def main() -> None:
    cred = credentials.Certificate(SERVICE_KEY)
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    for chat in SAMPLE_CHATS:
        chat_id = build_chat_id(chat["anonId"], chat["recipientEmail"])
        now_ms = int(time.time() * 1000)
        # 메타데이터 업서트
        meta = {
            "participants": [chat["anonId"], chat["recipientEmail"]],
            "recipientEmail": chat["recipientEmail"],
            "recipientName": chat["recipientName"],
            "lastMessage": chat["messages"][-1]["text"],
            "lastAt": now_ms,
        }
        db.collection("chats").document(chat_id).set(meta)

        # 메시지 시드
        for idx, m in enumerate(chat["messages"]):
            msg_payload = {
                "text": m["text"],
                "senderId": m["senderId"],
                "senderName": m["senderName"],
                "recipientName": chat["recipientName"],
                "recipientEmail": chat["recipientEmail"],
                "createdAt": now_ms + idx,
            }
            db.collection("chats").document(chat_id).collection("messages").add(msg_payload)

        print(f"seeded chat: {chat_id} -> {chat['recipientEmail']}")


if __name__ == "__main__":
    main()

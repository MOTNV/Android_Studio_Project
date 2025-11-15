import firebase_admin
from firebase_admin import auth, credentials

SERVICE_KEY = "serviceAccount.json"
PASSWORD = "123456"  # Firebase 요구사항(6자 이상)에 맞춘 공통 비밀번호

PROFESSOR_USERS = [
    ("djeong@kunsan.ac.kr", "정동원"),
    ("bwon@kunsan.ac.kr", "온병원"),
    ("leha82@kunsan.ac.kr", "이석훈"),
    ("cson@kunsan.ac.kr", "손창환"),
    ("jwgim@kunsan.ac.kr", "김장원"),
    ("junghj85@kunsan.ac.kr", "정현준"),
    ("nunghoi@kunsan.ac.kr", "김능회"),
    ("imnyj@kunsan.ac.kr", "남영주"),
    ("junma@kunsan.ac.kr", "마준"),
]


def main() -> None:
    cred = credentials.Certificate(SERVICE_KEY)
    firebase_admin.initialize_app(cred)

    for email, name in PROFESSOR_USERS:
        try:
            user = auth.get_user_by_email(email)
            print(f"exists: {email} (uid={user.uid})")
            # 비밀번호 갱신
            auth.update_user(user.uid, password=PASSWORD, display_name=name)
        except auth.UserNotFoundError:
            user = auth.create_user(
                email=email,
                password=PASSWORD,
                display_name=name,
            )
            print(f"created: {email} (uid={user.uid})")
        # 역할 커스텀 클레임 부여
        auth.set_custom_user_claims(user.uid, {"role": "respondent", "name": name})


if __name__ == "__main__":
    main()

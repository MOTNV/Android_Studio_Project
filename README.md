# AnonCounsel (익명 상담 메신저)

LLM 기반 스마트 라우팅과 하이브리드 암호화(AES-256 + RSA-2048)를 갖춘 익명 상담 메신저 안드로이드 앱입니다. 학생이 익명 ID로 민원을 작성하면 LLM 스텁이 카테고리/우선순위/추천 수신자를 분석하고, 메시지는 엔드투엔드 암호화 후 로컬 DB(Room)에 저장됩니다.

## 현재 구현
- MVVM 스켈레톤: `MessageViewModel` ↔ `MessageRepository` ↔ Room(`MessageDao`, `AppDatabase`)
- 암호화: `CryptoManager`로 AES-GCM 암호화 + RSA-OAEP 키 래핑
- LLM 라우팅 스텁: `LlmAnalyzer` + `RecipientDirectory` 키워드 기반 카테고리/수신자 추천
- UI: `MainActivity` 단일 화면  
  - 상단 LLM 라우팅 미리보기(추천 수신자, 카테고리, 우선순위, 키워드)  
  - 메시지 리스트(암호화된 DB → 복호화 후 표시), 빈 상태 안내  
  - 하단 입력창 + 전송 버튼

## 구조
- `app/src/main/java/com/non_breath/finlitrush/`
  - `MainActivity` : 바인딩 + 뷰모델 연결
  - `ui/MessageViewModel`, `ui/MessageAdapter`
  - `repository/MessageRepository`
  - `data/` : `AppDatabase`, `MessageDao`, `MessageEntity`
  - `crypto/CryptoManager`
  - `llm/` : `LlmAnalyzer`, `RecipientDirectory`
  - `model/` : `Message`, `AnalysisResult`, `IdentityProvider`, `Recipient`, `EncryptionResult`

## 빌드/실행
```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```
Android Studio에서 열고 `Run > Run 'app'` 실행. minSdk 24 / targetSdk 36 / Java 11.

## 다음 단계(제안서 대비)
- Firebase(Firestore, Auth, FCM, Functions) 실연동 및 메시지 자동 파기(30일) Cloud Functions 구현  
  - `app/google-services.json` 추가(예시: `app/google-services.json.example`) 후 빌드하면 Firestore 업로드 스텁이 활성화됩니다.
- 파일/이미지 첨부 + EXIF 제거 + 크기 패딩 처리
- 알림 채널/토큰 관리, 관리자용 통계 대시보드
- 온디바이스 생체 인증 + Room 암호화/백업 정책 강화
- 실 LLM API(Claude) 연동 및 프롬프트/시스템 정책 적용, 사용자 동의 플로우

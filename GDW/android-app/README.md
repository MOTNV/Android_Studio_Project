# Android Client Skeleton

한국어 주석: Android Studio에서 열어 익명 편지 앱을 구현할 수 있는 뼈대입니다.

## 구조
- `app/src/main/java/com/kunsan/anonletters/MainActivity.kt` – ViewBinding 기반 메인 화면
- `res/layout/activity_main.xml` – 제출/검증 카드가 있는 간단한 UI
- `app/build.gradle` – Kotlin(Android) 플러그인, Material/ConstraintLayout 의존성 포함

## 시작하기
1. Android Studio에서 `android-app` 폴더를 열어 Gradle Sync를 수행합니다.
2. 필요 시 `local.properties`에 SDK 경로를 지정합니다.
3. 프로젝트가 동기화되면 `app` 구성을 선택하고 에뮬레이터/실기기에 `Run` 합니다.
4. 이후 Retrofit, Compose 등 원하는 스택으로 기능을 확장하세요.

## 다음 단계 제안
- Retrofit + Kotlinx Serialization을 추가해 `server/` API와 통신
- WorkManager/Coroutine으로 PoW 채굴을 백그라운드 처리
- Compose 또는 Navigation Component로 제출/검증 플로우 구현

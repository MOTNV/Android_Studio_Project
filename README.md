# FinLitRush – 2D Tile Demo (Android)

간단한 2D 타일 기반(쯔꾸르 스타일) 데모를 추가했습니다.

핵심 기능
- 화면 내 D‑패드로 플레이어 이동
- A 버튼으로 인접한 NPC와 대화 시작/종료
- 맵/에셋은 예시(도형/색상)로 구현 — 외부 리소스 없음

주요 파일
- `app/src/main/java/com/non_breath/finlitrush/MainActivity.java`
  - `GameView`를 화면에 붙이고 생명주기에서 일시정지/재개 처리
- `app/src/main/java/com/non_breath/finlitrush/game/GameView.java`
  - 게임 루프(SurfaceView + Thread)
  - 타일 맵 렌더링, 충돌, NPC 대화, 가상 버튼(D‑패드, A)

조작법
- 하단 좌측: 가상 조이스틱(이동) — D‑패드 대신 조작
- 하단 우측: A 버튼(인접한 NPC와 대화 시작/종료)
- 키보드(에뮬레이터): 방향키/WASD 이동, Space/Z/A 대화

빌드 방법
1) Android Studio 권장
   - Android Studio에서 프로젝트 열기 → Build > Make Project
   - 실행: Run ‘app’ (에뮬레이터/실기기)
2) Gradle CLI
   - Java 17+ 필요 (Gradle 8.13 / AGP 8.13)
   - Windows: `gradlew.bat assembleDebug`
   - macOS/Linux: `./gradlew assembleDebug`
   - 결과 APK: `app/build/outputs/apk/debug/`

교체/확장 포인트
- 타일 맵/에셋: JSON 맵 포맷 지원(기본) + Tiled(JSON, 단일 레이어/타일셋 지원)
  - 기본 JSON 예: `app/src/main/assets/maps/demo_map.json`
    - `tileset.type = "android-drawables"` 일 때 `drawables` 배열의 이름을 리소스와 매칭
    - `palette` 배열(hex 색상)로도 타일 생성 가능
    - `atlas`(이미지 경로, tileW/H, columns)도 지원(assets에 이미지 추가 필요)
  - 다층 레이어 & 충돌
    - 기본 JSON: `collision`(0/1 배열)로 충돌 정의, 미지정 시 `tiles`의 0이외 값을 벽으로 간주
    - Tiled(JSON): `layers` 중 이름이 `collision`(대소문자 무관)인 타일 레이어를 충돌로 처리하며 나머지는 드로잉 레이어로 렌더
- UI: 버튼 배치/크기, 폰트 크기, 다이얼로그 스타일링 커스터마이즈 가능
- 게임 로직: 상호작용, 맵 전환, 이벤트/퀘스트 등 확장

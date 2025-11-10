# AGENTS.md — Codex CLI 행동 규칙서

이 문서는 Codex CLI가 이 리포지토리에서 일관성 있게 코드를 생성하고 명령을 수행하도록 하는 운영 가이드입니다.

## Project Structure & Module Organization

이 저장소는 세 가지 주요 폴더로 구성됩니다. `server/`는 Express 기반 API와 데이터 스키마, 환경 템플릿을 포함하며 모든 런타임 소스가 여기에 있습니다. `contracts/`는 온체인 상호작용을 위한 Solidity 아티팩트를 보관하므로 서버에서 참조하는 계약 주소를 업데이트할 때 이 디렉터리를 확인하세요. `docs/`는 사용자 또는 운영 가이드를 담으며, 새 기능을 출시할 때 필요한 다이어그램과 결정 기록을 추가합니다. 루트에는 `docker-compose.yml`이 있어 Postgres/Redis 개발 인프라를 통합 관리합니다.

## Security & Configuration Tips

민감한 키는 `.env`에만 보관하고, 샘플 값은 `.env.example`에 업데이트하여 신규 기여자가 필요한 키 목록을 즉시 확인할 수 있게 합니다. Redis와 Postgres는 로컬 개발에서만 노출하되, 원격 배포 시 VPC 또는 Private Link를 통해 접근을 제한합니다. 온체인 앵커링용 RPC 키는 프로젝트별 전용 계정을 사용하고, 실패 시 로깅에 키나 개인 정보를 남기지 말아야 합니다.

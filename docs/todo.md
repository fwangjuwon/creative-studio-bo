# TODO

> 상태: 🟡 Step 3 진행 중
> 최종 업데이트: 2026-02-26

---

## 진행 중

### Step 3. 기능 개발 (Security 없이)
- [ ] User CRUD (`/api/users`)
  - [ ] entity, mapper, service, controller, dto
  - [ ] `UserMapper.xml` 작성
  - [ ] 비밀번호 BCrypt 암호화 (Security 없이 `BCryptPasswordEncoder` 직접 사용)
- [ ] Category CRUD (`/api/categories`)
  - [ ] entity, mapper, service, controller, dto
  - [ ] `CategoryMapper.xml` 작성
  - [ ] 단계별 드릴다운 조회 (parent_id 기반)
- [ ] Tag / TagGroup CRUD (`/api/tag-groups`, `/api/tags`)
  - [ ] entity, mapper, service, controller, dto
  - [ ] `TagGroupMapper.xml`, `TagMapper.xml` 작성
- [ ] Prompt 생성 (`/api/prompts`)
  - [ ] Gemini API 연동 서비스 (Spring RestClient 사용)
  - [ ] 일일 사용량 체크 (PromptHistory COUNT)
  - [ ] `PromptHistoryMapper.xml`, `PromptHistoryTagMapper.xml` 작성
- [ ] Prompt 히스토리 조회

---

## 완료

### Step 2. 프로젝트 의존성 / 설정
- [x] `build.gradle` 의존성 추가
  - [x] `mybatis-spring-boot-starter:3.0.5` 주석 해제
  - [x] `postgresql` 드라이버 주석 해제
  - [x] Gemini — Spring RestClient 사용 (별도 SDK 없음, starter-web 포함)
  - [x] ~~`spring-boot-starter-security`~~ — Step 4에서 추가
  - [x] ~~JWT 라이브러리 (`jjwt`)~~ — Step 4에서 추가
- [x] `src/main/resources/mybatis-config.xml` 작성 (`mapUnderscoreToCamelCase=true` 포함)
- [x] `src/main/resources/application.yml` 작성 (공통 설정, `spring.profiles.active: local` 기본값)
- [x] `src/main/resources/application-local.yml` 작성 (로컬 DB 연결, `${...}` 참조 방식)

### Step 1. 로컬 DB 준비
- [x] `.env` 파일 생성 (`.env.example` 복사 후 실제 값 입력, 커밋 금지)
- [x] `Dockerfile.postgres` 작성
- [x] `docker-compose.yml` 작성 (`env_file` 연동 완료)
- [x] `docs/erd.md` 작성 (테이블 정의, 컬럼 설명, mermaid ERD)
- [x] `db/schema.sql` 작성 (DDL — 테이블 6개, `erd.md` 기반)
- [x] `db/data.sql` 작성 (초기 데이터 — ADMIN 계정, 기초 카테고리/태그)
- [x] `docker-compose.yml` SQL 마운트 추가 (`docker-entrypoint-initdb.d`)
- [x] 로컬 PostgreSQL 컨테이너 최초 실행 및 DB 연결 확인 (DBeaver로 ADMIN 데이터 확인)

### 문서화
- [x] `docs/requirements.md` — 요구사항 명세 작성
- [x] `CLAUDE.md` — Claude Code / Claude AI 진입점 문서 작성
- [x] `GEMINI.md` — Gemini CLI 진입점 문서 작성
- [x] `.cursor/rules/project-context.mdc` — 프로젝트 도메인 컨텍스트 작성
- [x] `.cursor/rules/coding-convention.mdc` — 코딩 컨벤션 작성
- [x] `docs/todo.md` — 개발 진행 현황 문서 작성

### 스펙 확정 (2026-02-26)
- [x] API URL prefix 확정: `/api/{domain}`
- [x] JWT 스펙 확정: Access Token 1시간, Refresh Token 없음 (PoC)
- [x] Gemini API Key 관리 방식 확정: `.env` 파일, `GEMINI_API_KEY`
- [x] 카테고리 조회 흐름 확정: 단계별 드릴다운 (LCS → MCS → SCS → DCS)
- [x] 페이지네이션 정책 확정: PoC 단계 없음, 추후 PageHelper 적용
- [x] 공통 응답 형식 확정: `CommonResponse<T>(int code, String message, T data)` (HTTP 상태코드)
- [x] `mybatis-config.xml` 직접 작성 예정 (AI 생성 안 함)
- [x] 프롬프트 히스토리 필터 — 추후 구현
- [x] 환경변수 전략 확정: `.env.example` 커밋, `.env` gitignore, Spring Profile (`local` 기본값)
- [x] `.env.example` 파일 생성 완료
- [x] `.gitignore` 에 `.env` 추가 완료
- [x] Security 개발 순서 확정: Step 4에서 일괄 추가 (비즈니스 로직 먼저, 인증 나중)

---

## 예정

### Step 4. Security 추가 (기능 개발 완료 후)
- [ ] `build.gradle` 의존성 추가
  - [ ] `spring-boot-starter-security`
  - [ ] `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
  - [ ] `spring-security-test`
- [ ] `SecurityConfig` 작성 (JWT 필터 체인)
- [ ] `JwtTokenProvider` 작성 (토큰 발급 / 검증)
- [ ] `JwtAuthFilter` 작성 (요청마다 토큰 검증)
- [ ] `UserDetailsService` 구현 (User 도메인 연동)
- [ ] 로그인 API (`POST /api/auth/login`) 구현
- [ ] 각 API Role 기반 접근 제어 (`@PreAuthorize`)

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](requirements.md)
- 프로젝트 진입점: [`CLAUDE.md`](../CLAUDE.md)

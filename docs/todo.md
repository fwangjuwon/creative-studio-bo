# TODO

> 상태: 🟡 Step 3 진행 중
> 최종 업데이트: 2026-02-26 (Step 3-A-T 완료)

---

## 진행 중

### Step 3-A. User CRUD
- [x] `User` entity
- [x] `UserMapper` 인터페이스 + `UserMapper.xml`
- [x] `UserService`
- [x] `UserController` (`/api/users`)
- [x] `UserCreateReqDto`, `UserUpdateReqDto`, `UserSearchReqDto`, `UserRespDto`
- [x] 비밀번호 BCrypt 암호화 (`BCryptPasswordEncoder` 직접 사용, Security 없이)
- [x] `common/exception` — `BizException`, `ErrorCode`(인터페이스), `GlobalExceptionHandler` (`NotFoundException`, `BusinessException` 제거)
- [x] `common/config/PasswordEncoderConfig` — `BCryptPasswordEncoder` Bean 등록

---

## 예정

### Step 3-A-T. User Service 단위 테스트
- [x] `UserServiceTest` 작성
  - [x] 회원 생성 — 비밀번호 BCrypt 암호화 검증
  - [x] 회원 생성 — 중복 loginId 처리
  - [x] 회원 단건 조회 — 존재 / 미존재
  - [x] 회원 목록 조회 (전체 / 조건별 필터링 / 이름 LIKE 검색)
  - [x] 회원 수정 (이름·권한·팀명·한도 / 비밀번호 변경 BCrypt 재검증 / 비밀번호 null 시 기존 유지 / 미존재 예외)
  - [x] 회원 삭제 (삭제 후 미존재 확인 / 미존재 예외)
- [x] `UserServiceTests` Suite 작성
- [x] `AllServiceTests` Suite 작성
- [x] `AllLayersTests` Suite 작성

### Step 3-B. Category CRUD
- [ ] `Category` entity + `CategoryType` enum
- [ ] `CategoryMapper` 인터페이스 + `CategoryMapper.xml`
- [ ] `CategoryService`
- [ ] `CategoryController` (`/api/categories`)
- [ ] `CategoryCreateReqDto`, `CategoryUpdateReqDto`, `CategoryRespDto`
- [ ] 단계별 드릴다운 조회 (parent_id 기반)

### Step 3-B-T. Category Service 단위 테스트
- [ ] `CategoryServiceTest` 작성
  - [ ] 카테고리 생성
  - [ ] 단계별 드릴다운 조회 — LCS / MCS / SCS / DCS
  - [ ] 수정 / 삭제

### Step 3-C. TagGroup / Tag CRUD
- [ ] `TagGroup`, `Tag` entity
- [ ] `TagGroupMapper`, `TagMapper` 인터페이스 + XML
- [ ] `TagGroupService`, `TagService`
- [ ] `TagGroupController` (`/api/tag-groups`), `TagController` (`/api/tags`)
- [ ] 관련 Req/Resp DTO

### Step 3-C-T. TagGroup / Tag Service 단위 테스트
- [ ] `TagGroupServiceTest` 작성
  - [ ] TagGroup 생성 / 수정 / 삭제
  - [ ] TagGroup 목록 조회
- [ ] `TagServiceTest` 작성
  - [ ] Tag 생성 / 수정 / 삭제
  - [ ] TagGroup별 Tag 목록 조회

### Step 3-D. Prompt 생성 + 히스토리
- [ ] `PromptHistory`, `PromptHistoryTag` entity
- [ ] `PromptHistoryMapper`, `PromptHistoryTagMapper` 인터페이스 + XML
- [ ] Gemini API 연동 서비스 (Spring RestClient 사용)
- [ ] 일일 사용량 체크 (PromptHistory COUNT)
- [ ] `PromptController` (`/api/prompts`)
- [ ] 히스토리 조회 (USER: 같은 팀, MANAGER/ADMIN: 전체)

### Step 3-D-T. Prompt Service 단위 테스트
- [ ] `PromptServiceTest` 작성
  - [ ] 일일 사용량 체크 — 한도 미만 → 허용
  - [ ] 일일 사용량 체크 — 한도 초과 → 거부
  - [ ] 히스토리 조회 — USER: 같은 팀만
  - [ ] 히스토리 조회 — MANAGER/ADMIN: 전체

---

## 완료

### 테스트 환경 설정
- [x] `build.gradle` — `test` 태스크에 `.env` 로딩 추가 (Gradle 실행 시 환경변수 자동 주입)
- [x] `application.yml` — `GEMINI_API_KEY` 기본값 설정 (`${GEMINI_API_KEY:}`)
- [x] IntelliJ `Run tests using: Gradle` 로 변경

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

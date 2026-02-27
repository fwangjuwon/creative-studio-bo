# TODO

> 상태: 🟡 Step 3 완료, Step 4 대기
> 최종 업데이트: 2026-02-27 (Step 3-D 보강 — DCS 검증, 카테고리 전체경로, 빈응답 예외처리 등)

---

### Step 3-D. Prompt 생성 + 히스토리
- [x] `PromptHistory`, `PromptHistoryTag` entity
- [x] `PromptHistoryMapper`, `PromptHistoryTagMapper` 인터페이스 + XML
- [x] `GeminiProperties` (`@ConfigurationProperties(prefix = "gemini")`) — apiKey, model, baseUrl
- [x] `GeminiConfig` — `RestClient` Bean 등록
- [x] `GeminiService` + `GeminiServiceImpl` (Spring RestClient, 내부 DTO는 private record)
  - [x] 시스템 프롬프트 — `GeminiServiceImpl` 내 `private static final String SYSTEM_PROMPT` 상수 (비즈니스 로직)
  - [x] Gemini 응답 비어있을 때 `BizException(P006)` 발생 (빈 프롬프트 저장 방지)
  - ℹ️ 추후 Spring RestClient → Google AI 공식 라이브러리로 교체 예정
- [x] `PromptErrorCode` (P001 일일한도초과, P002 유저미존재, P003 카테고리미존재, P004 태그미존재, P005 DCS아닌카테고리, P006 Gemini빈응답)
- [x] `PromptService` + `PromptServiceImpl`
  - [x] 일일 한도 체크, Gemini 호출, 히스토리 저장 (`@Transactional`)
  - [x] DCS 카테고리 타입 검증 (`INVALID_CATEGORY_TYPE` P005)
  - [x] 카테고리 전체 경로 조합 후 Gemini 전달 (`식품 > 건강식품 > 약재 > 인삼`)
- [x] `PromptController` (`/api/prompts`) — POST 생성, GET 조회
- [x] 히스토리 조회 권한 분기 — USER: 같은 팀 / 팀 미배정 USER는 빈 목록 반환, MANAGER/ADMIN: 전체
- [x] `application.yml` — model: `gemini-3-flash-preview` (system-prompt는 코드로 이전)

### Step 3-D-T. Prompt Service 단위 테스트
- [x] `PromptServiceTest` 작성 (`@MockBean GeminiService` 모킹)
  - [x] 프롬프트 생성 — 일일 한도 미만 → 허용 (태그 포함 / 태그 없이)
  - [x] 프롬프트 생성 — 일일 한도 초과 → 거부 (`P001`)
  - [x] 프롬프트 생성 — 유저 미존재 예외 (`P002`)
  - [x] 프롬프트 생성 — 카테고리 미존재 예외 (`P003`)
  - [x] 프롬프트 생성 — DCS가 아닌 카테고리 선택 예외 (`P005`)
  - [x] 프롬프트 생성 — Gemini에 LCS→DCS 전체 경로 전달 검증 (`ArgumentCaptor`)
  - [x] 히스토리 조회 — USER: 같은 팀만
  - [x] 히스토리 조회 — USER 팀 미배정: 빈 목록 반환
  - [x] 히스토리 조회 — MANAGER: 전체
  - [x] 히스토리 조회 — ADMIN: 전체
  - [x] 히스토리 조회 — 유저 미존재 예외 (`P002`)
- [x] `PromptServiceTests` Suite 작성
- [x] `AllServiceTests` — `PromptServiceTests` 추가

---

## 완료

### Step 3-C. TagGroup / Tag CRUD
- [x] `TagGroup`, `Tag` entity
- [x] `TagGroupMapper`, `TagMapper` 인터페이스 + XML
- [x] `TagGroupService`, `TagService` (인터페이스 + 구현체)
- [x] `TagGroupController` (`/api/tag-groups`), `TagController` (`/api/tags`)
- [x] `TagGroupCreateReqDto`, `TagGroupUpdateReqDto`, `TagGroupRespDto`
- [x] `TagCreateReqDto`, `TagUpdateReqDto`, `TagRespDto`
- [x] `TagGroupErrorCode` (TG001 미존재, TG002 소속태그존재), `TagErrorCode` (T001 미존재, T002 그룹 미존재)
- [x] Tag 생성 시 TagGroup 존재 여부 검증
- [x] TagGroup 삭제 시 소속 Tag 존재하면 거부 (`TAG_GROUP_HAS_TAGS` TG002) — `TagMapper.existsByTagGroupId` 추가

### Step 3-C-T. TagGroup / Tag Service 단위 테스트
- [x] `TagGroupServiceTest` 작성
  - [x] TagGroup 생성 — 단일/복수 선택
  - [x] TagGroup 단건 조회 — 존재 / 미존재
  - [x] TagGroup 전체 목록 조회
  - [x] TagGroup 수정 (이름·isMultiSelect·정렬순서 / 미존재 예외)
  - [x] TagGroup 삭제 — 소속 태그 없을 때 허용 / 소속 태그 존재 시 거부 / 미존재 예외
- [x] `TagServiceTest` 작성
  - [x] Tag 생성 — 정상 케이스
  - [x] Tag 생성 — 존재하지 않는 TagGroup 예외
  - [x] Tag 단건 조회 — 존재 / 미존재
  - [x] Tag 목록 조회 — TagGroup별 필터링 / 전체 / 그룹 격리 검증
  - [x] Tag 수정 (이름·정렬순서 / 미존재 예외)
  - [x] Tag 삭제 (삭제 후 미존재 확인 / 미존재 예외)
- [x] `TagServiceTests` Suite 작성
- [x] `AllServiceTests` — `TagServiceTests` 추가

### Step 3-B. Category CRUD
- [x] `Category` entity + `CategoryType` enum
  - [x] `categoryType` 필드 `String` → `CategoryType` enum으로 변경 (타입 안정성)
- [x] `CategoryMapper` 인터페이스 + `CategoryMapper.xml`
  - [x] `existsByParentId(Long parentId)` — 자식 카테고리 존재 여부 확인
  - [x] `selectCategoryPath(Long id)` — 재귀 CTE로 LCS→DCS 전체 경로 조회
- [x] `CategoryService` (인터페이스 + 구현체)
  - [x] 생성 시 부모 타입 계층 유효성 검증 (`INVALID_CATEGORY_HIERARCHY` C003)
  - [x] 삭제 시 자식 카테고리 존재하면 거부 (`CATEGORY_HAS_CHILDREN` C004)
- [x] `CategoryController` (`/api/categories`)
- [x] `CategoryCreateReqDto` — `categoryType` 필드 `@Pattern` → `@NotNull CategoryType` enum
- [x] `CategoryUpdateReqDto`, `CategoryRespDto`
- [x] 단계별 드릴다운 조회 (parent_id 기반)
- [x] `CategoryErrorCode` enum (C001 미존재, C002 부모미존재, C003 계층오류, C004 자식존재시삭제불가)

### Step 3-B-T. Category Service 단위 테스트
- [x] `CategoryServiceTest` 작성
  - [x] 카테고리 생성 — LCS (parentId null)
  - [x] 카테고리 생성 — 하위 카테고리 (MCS/SCS/DCS)
  - [x] 카테고리 생성 — 계층 구조 위반 예외 (LCS에 parentId 입력 / MCS·SCS·DCS에 parentId null)
  - [x] 카테고리 생성 — 잘못된 부모 타입 예외 (`C003`)
  - [x] 카테고리 생성 — 상위 카테고리 미존재 예외
  - [x] 단건 조회 — 존재 / 미존재
  - [x] 단계별 드릴다운 조회 — LCS / 하위 카테고리 / 4단계 전체
  - [x] 수정 (이름·정렬순서 / 미존재 예외)
  - [x] 삭제 (삭제 후 미존재 확인 / 미존재 예외)
  - [x] 삭제 — 자식 카테고리 존재 시 거부 (`C004`)
- [x] `CategoryServiceTests` Suite 작성
- [x] `AllServiceTests` — `CategoryServiceTests` 추가

### Step 3-A. User CRUD
- [x] `User` entity
- [x] `UserMapper` 인터페이스 + `UserMapper.xml`
- [x] `UserService`
- [x] `UserController` (`/api/users`)
- [x] `UserCreateReqDto`, `UserUpdateReqDto`, `UserSearchReqDto`, `UserRespDto`
- [x] 비밀번호 BCrypt 암호화 (`BCryptPasswordEncoder` 직접 사용, Security 없이)
- [x] `common/exception` — `BizException`, `ErrorCode`(인터페이스), `GlobalExceptionHandler` (`NotFoundException`, `BusinessException` 제거)
- [x] `common/config/PasswordEncoderConfig` — `BCryptPasswordEncoder` Bean 등록

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

# CLAUDE.md

이 파일은 Claude Code / Claude AI가 프로젝트를 이해하기 위한 진입점 문서입니다.
코드 작성 전 반드시 이 문서와 `docs/requirements.md` 를 참조하세요.

---

## 프로젝트 개요

**ns-creative-studio-bo** — 이미지 생성형 AI용 프롬프트 자동 생성 서비스의 백엔드 API 서버

사용자가 **상품 카테고리(어떤 상품)** 와 **이미지 스타일 태그(어떻게 보일지)** 를 선택하면,
Gemini API를 통해 최적화된 **영문 이미지 생성 프롬프트**를 자동 생성한다.
생성된 프롬프트는 팀 내에서 공유되어 재사용할 수 있다.

> 상품 프롬프트는 텍스트만 생성하며 이미지 생성은 사용자가 직접 수행한다. 인물 프롬프트는 NanoBanana(`gemini-3-pro-image-preview`)를 통한 이미지 직접 생성도 지원한다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle |
| Base Package | `com.nsmall.ns_creative_studio_bo` |
| DB | PostgreSQL |
| ORM | MyBatis |
| 인증 | Spring Security + JWT (Stateless, 완료) |
| AI | Gemini API (`gemini-3-flash-preview` 기본) + NanoBanana (`gemini-3-pro-image-preview`, 인물 이미지 생성) |
| API 문서 | Swagger UI (`springdoc-openapi-starter-webmvc-ui:2.8.14`) |

---

## 빌드 / 실행

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun
```

---

## 패키지 구조

```
com.nsmall.ns_creative_studio_bo
├── domain
│   ├── user
│   ├── category
│   ├── tag
│   └── prompt
├── common
│   ├── config        # MyBatis, Gemini, Security 설정
│   ├── exception     # 공통 예외 처리
│   ├── util
│   └── CommonResponse.java  # 공통 API 응답 래퍼 (서브패키지 없이 바로)
└── NsCreativeStudioBoApplication.java
```

각 도메인 패키지 내부 구조:
```
{domain}
├── controller
├── service
│   ├── {Domain}Service.java        # 인터페이스
│   └── {Domain}ServiceImpl.java    # 구현체 (@Service)
├── mapper                          # MyBatis Mapper 인터페이스
├── dto
│   ├── request
│   └── response
├── entity                          # DB 테이블 매핑 객체
└── {Domain}ErrorCode.java          # 도메인 에러 코드 enum
```

---

## 핵심 도메인 개념

### 권한(Role) — 계층 구조
```
ADMIN > MANAGER > USER
```
| Role | 주요 권한 |
|------|---------|
| `ADMIN` | 모든 기능 풀 접근, 유저 생성/관리, daily_limit 설정 |
| `MANAGER` | USER 권한 + 카테고리·태그·인물프롬프트(Step/Attr/Option) CUD |
| `USER` | 프롬프트 생성(하루 N회), 전체 프롬프트 조회 |

- 회원가입 없음. ADMIN이 API를 통해 직접 계정 생성
- 팀 개념: `User.team_name` VARCHAR 컬럼으로 관리 (별도 Team 테이블 없음)

### 카테고리 vs 태그 — 완전히 다른 개념

**카테고리** (어떤 상품을 촬영/생성할지)
- 3단계 계층: LCS(대) > MCS(중) > SCS(소)
- self-reference 구조 (`parent_id`)
- ex: `식품 > 건강식품 > 인삼`

**태그** (이미지 스타일/분위기)
- TagGroup → Tag 구조
- 그룹별 단일/복수 선택 여부 다름 (`is_multi_select`)
- ex: `인물(단일) → 남자/여자`, `분위기(복수) → 화사한/추운/따뜻한`

### 카테고리 조회 흐름 (단계별 드릴다운)
```
LCS 목록 조회 → MCS 목록 조회(parent_id=LCS.id) → SCS 최종 선택
```
사용자는 SCS를 최종 선택하여 프롬프트 생성에 사용

### 프롬프트 생성 흐름
```
카테고리(SCS) 선택 + 태그 선택 → Gemini API → 영문 프롬프트 반환 → PromptHistory 저장
```

### 사용량 체크 (배치 없음)
```
요청 시 PromptHistory COUNT(오늘, 해당 유저) >= User.daily_limit → 거부
```

---

## API 스펙 요약

| 항목 | 내용 |
|------|------|
| URL prefix | `/api/{domain}` (예: `/api/users`, `/api/categories`) |
| 공통 응답 | `CommonResponse<T>(int code, String message, T data)` |
| Controller 반환 타입 | `CommonResponse<T>` 직접 반환 — `ResponseEntity` 사용 금지 |
| 성공 HTTP 상태 | **무조건 200** — CUD/READ 구분 없음, 201/204 사용 금지 |
| 조회 결과 없음 | 200 + `data: null` — 404 사용 안 함 (내부 API 개발 편의성) |
| 페이지네이션 / 필터 | `GET /api/prompts` PageHelper 페이징 + 검색 필터 적용 완료. `pageNum`(기본 1) / `pageSize`(기본 10). 검색 조건(모두 선택): `loginId`(생성자), `promptType`(`PRODUCT`\|`PERSON`), `promptStyle`(`NANO_BANANA`\|`MID_JOURNEY`), `dateFrom`/`dateTo`(생성일 범위). 공통 모듈: `common/page/` |
| JWT | 완료 (Access Token 1시간, Refresh Token 없음, PoC) |
| Gemini API Key | `.env` 파일의 `GEMINI_API_KEY` 환경변수 |
| 이미지 서빙 | 생성 이미지는 `image.storage-path` 경로에 저장, `image.base-url` prefix로 절대 URL 조립 후 응답. DB에는 `/images/{uuid}.ext` 상대 경로만 저장. `/images/**` 경로는 Security 인증 없이 공개. |
| CORS | `local` 프로필에서만 Spring Boot `CorsConfig` 활성화 (`@Profile("local")`). dev/prod 환경은 APIM에서 CORS 처리. 허용 Origin은 `cors.allowed-origins`로 환경별 관리. |
| imageAspectRatio | 상품·인물 프롬프트 생성 시 이미지 비율 선택 가능 (`1:1`, `4:3`, `16:9`, `9:16`). Gemini 응답 후 Java에서 format suffix 주입. |

---

## DB 설계 원칙

- **PK**: `BIGINT GENERATED ALWAYS AS IDENTITY` (sequence)
- **FK 제약조건**: DB에 생성하지 않음. 논리적 관계만 유지, 정합성은 Service에서 보장
- **공통 컬럼** (DB: snake_case → Java: camelCase 자동 매핑):
  - `init_regi_pic` / `init_regi_dttm` — 등록자(login_id) / 등록일시
  - `final_rvis_pic` / `final_rvis_dttm` — 최종수정자(login_id) / 최종수정일시
- **카테고리 계층 구분**: `category_type` 컬럼 (`LCS` / `MCS` / `SCS`)

> ⚠️ `createdAt`, `updatedAt` 사용 금지. 반드시 위의 공통 컬럼 네이밍을 사용할 것.

---

## 주요 엔티티 요약

| 엔티티 | 테이블명 | 설명 |
|--------|---------|------|
| User | `user` | 회원 정보, 권한, 팀명, 일일 한도 |
| Category | `category` | 상품 카테고리 계층 (LCS~SCS) |
| TagGroup | `tag_group` | 태그 묶음 (인물, 분위기 등) |
| Tag | `tag` | 태그 값 (남자, 화사한 등) |
| PromptHistory | `prompt_history` | 생성된 프롬프트 저장 (상품/인물 공통) |
| GoodsPromptHistoryTag | `goods_prompt_history_tag` | 상품 프롬프트-태그 매핑 |
| HumanPromptHistoryOption | `human_prompt_history_option` | 인물 프롬프트-선택옵션 매핑 |
| HumanPromptStep | `human_prompt_step` | 인물 프롬프트 5단계 구조 (A~E) |
| HumanPromptAttr | `human_prompt_attr` | 단계별 세부항목 (연령대, 성별 등) |
| HumanPromptOption | `human_prompt_option` | 세부항목별 선택지 (prompt_value 포함) |

---

## 코딩 컨벤션 핵심 규칙

> 세부 규칙 전체는 `.cursor/rules/coding-convention.mdc` 참조

### Service 레이어 패턴

모든 Service는 **인터페이스 + 구현체** 패턴으로 작성한다.

```java
// ✅ 인터페이스
public interface UserService { ... }

// ✅ 구현체
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService { ... }

// Controller 주입 — 반드시 인터페이스 타입으로
private final UserService userService;
```

### JavaDoc

모든 `.java` 파일 최상단에 JavaDoc 필수.

```java
/**
 * 클래스 설명
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
```

### Swagger API 문서화

`@RestController` 메서드는 **JavaDoc 주석 대신 Swagger 어노테이션**으로 문서화한다.
클래스 레벨 JavaDoc (`@author`, `@since`)은 별개로 유지.

```java
@Tag(name = "User", description = "회원 관리 API")   // 클래스
@Operation(summary = "회원 생성", description = "...")  // 메서드
@ApiResponse(responseCode = "201", description = "...")
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### MyBatis Type Alias

`application.yml`에 `type-aliases-package: com.nsmall.ns_creative_studio_bo.domain` 설정.
Mapper XML에서 **전체 패키지 경로 사용 금지** — 클래스 짧은 이름만 사용.

```xml
<!-- ✅ -->
<select id="selectById" resultType="User">
<!-- ❌ -->
<select id="selectById" resultType="com.nsmall.ns_creative_studio_bo.domain.user.entity.User">
```

### MyBatis XML 특수문자 처리 — 인라인 CDATA

Mapper XML에서 `<`, `>`, `<>` 등 XML 특수문자가 포함된 SQL 연산자는 **인라인 CDATA**로 감싼다.

```xml
<!-- ✅ 연산자만 인라인 CDATA로 감싸기 -->
SELECT COUNT(*) <![CDATA[ > ]]> 0
WHERE age <![CDATA[ < ]]> 30

<!-- ❌ 전체 SQL CDATA 감싸기 금지 -->
<![CDATA[ SELECT COUNT(*) > 0 FROM ... ]]>

<!-- ❌ 엔티티 이스케이프 금지 -->
SELECT COUNT(*) &gt; 0
```

### MyBatis Mapper 메서드 네이밍 — 목록 조회

목록 조회 메서드는 파라미터 유무와 무관하게 **`selectList`로 통일**한다.

```java
// ✅ 파라미터 없는 전체 조회도 selectList
List<TagGroup> selectList();

// ✅ 파라미터 있는 필터 조회도 동일
List<Category> selectList(@Param("parentId") Long parentId);

// ❌ 금지
List<TagGroup> selectAll();
```

예외: 조회 대상을 명확히 설명하는 복합 이름은 허용한다.

```java
// ✅ 예외 — 3테이블 조인 범위를 이름으로 명시
List<HumanPromptStepRespDto> selectAllWithAttrsAndOptions();
```

### MyBatis @Param 규칙

`@Param` 적용 기준:

- **primitive / wrapper 단일 파라미터** (`Long id`, `String loginId` 등) → `@Param` **필수**
  - `@Param` 없어도 현재는 동작하지만, 파라미터 추가 시 XML `#{변수명}` 매핑이 런타임에 조용히 깨짐 (컴파일 타임 미탐지)
- **Entity / DTO 단일 객체** (`User user`, `UserSearchReqDto dto`) → `@Param` **불필요**
  - 붙이면 XML에서 `#{user.id}`, `#{dto.name}` 형태로 바꿔야 해서 오히려 복잡해짐

```java
// ✅ primitive/wrapper — @Param 필수
Optional<User>  selectById(@Param("id") Long id);
List<Category>  selectList(@Param("parentId") Long parentId);
void            deleteById(@Param("id") Long id);

// ✅ Entity/DTO 단일 객체 — @Param 불필요
void insert(User user);
void update(Category category);
List<User> selectList(UserSearchReqDto dto);

// ❌ primitive에 @Param 누락
Optional<User>  selectById(Long id);
void            deleteById(Long id);
```

### Java FQN 사용 금지

Java 코드 생성 시 메서드 바디나 필드 선언 내에 **FQN(전체 패키지 경로)을 직접 사용하지 않는다.**
반드시 파일 상단 import 블록에 선언하고 짧은 클래스명만 사용한다.

```java
// ❌ 금지 — 메서드 바디 내 FQN 직접 사용
java.util.stream.IntStream.range(0, 3)
java.util.concurrent.CompletableFuture.supplyAsync(task)
} catch (java.io.IOException e) {
throw new BizException(com.example.project.domain.SomeErrorCode.SOME_ERROR);

// ✅ 사용 — import 후 짧은 이름 사용
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

IntStream.range(0, 3)
CompletableFuture.supplyAsync(task)
} catch (IOException e) {
throw new BizException(SomeErrorCode.SOME_ERROR);
```

### 예외 처리 구조

- `ErrorCode` 인터페이스 → 도메인별 enum으로 구현 (예: `UserErrorCode`)
- `BizException(ErrorCode)` — 비즈니스 예외 발생 시 사용
- `errorCode`는 **로그에만** 사용, API 응답 body에 포함하지 않음

```java
throw new BizException(UserErrorCode.DUPLICATE_LOGIN_ID);
```

### 테스트 코드

- **Mapper 단독 테스트 작성 안 함** — ServiceTest가 DB 연동 포함 검증
- `@DisplayName` 및 메서드명은 **한글** 작성
- 네이밍: `[예상결과] 테스트 주제 - 세부설명` (예: `[성공] 회원 생성 - 중복 loginId`)
- CUD 테스트: `@Transactional` + `@Rollback` 필수
- 파라미터 여러 개: `@ParameterizedTest` + `@CsvSource` / `@ValueSource`
- 도메인별 테스트 완료 후 `@Suite`로 묶어 관리

> 상세 규칙: [`.cursor/rules/coding-convention.mdc`](.cursor/rules/coding-convention.mdc) — "테스트 코드 규칙" 섹션

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](docs/requirements.md)
- ERD 및 테이블 정의: [`docs/erd.md`](docs/erd.md)
- 개발 진행 현황: [`docs/todo.md`](docs/todo.md)
- 코딩 컨벤션 전체: [`.cursor/rules/coding-convention.mdc`](.cursor/rules/coding-convention.mdc)

# TODO

> 상태: ✅ refactor/prompt-history-remove-team-filter 완료
> 최종 업데이트: 2026-03-13 (refactor/prompt-history-remove-team-filter — 프롬프트 이력 조회 팀 필터 제거, 전체 조회로 단순화)

---

## 진행 중

없음

---

## 완료

### refactor/prompt-history-remove-team-filter. 프롬프트 이력 조회 팀 필터 제거

> Role 기반 팀 필터를 제거하여 모든 Role이 전체 이력을 조회하도록 단순화.
> USER → 같은 팀만 / MANAGER·ADMIN → 전체 의 분기 로직을 완전히 제거.

#### Mapper
- [x] `PromptHistoryMapper.java` — `selectList(@Param("teamName") String teamName)` → `selectList()` 파라미터 제거
- [x] `PromptHistoryMapper.xml` — `selectList` SQL에서 `JOIN "user"` + `WHERE u.team_name` 조건절 제거, 단순 전체 조회로 변경

#### Service
- [x] `PromptService` — `getPromptHistories(Long requestUserId)` → `getPromptHistories()` 시그니처 변경, JavaDoc 수정
- [x] `PromptServiceImpl` — `getPromptHistories` 내 Role 분기·User 조회·teamName 처리·`log.warn` 전체 제거

#### Controller
- [x] `PromptController` — `@Operation` description 수정, `@AuthenticationPrincipal` 파라미터 제거, 서비스 호출 변경

#### 테스트
- [x] `PromptServiceTest` — 권한별 범위 테스트 5개 삭제
  - `성공_히스토리_조회_USER_같은팀만`, `성공_히스토리_조회_MANAGER_전체`, `성공_히스토리_조회_ADMIN_전체`
  - `예외_히스토리_조회_사용자_미존재`, `성공_히스토리_조회_USER_팀_미배정_빈목록`
- [x] `PromptServiceTest` — `성공_히스토리_조회_전체_권한별_동일결과` 추가
  - ADMIN / MANAGER / USER 세 Role에서 `getPromptHistories()` 호출 시 결과가 동일함을 증명
  - 세 Role의 히스토리가 모두 결과에 포함되는지 검증
- [x] `HumanPromptServiceTest` — `getPromptHistories(user.getId())` → `getPromptHistories()` 호출 수정 (누락 반영)

#### 문서
- [x] `docs/requirements.md` — 조회 권한 테이블: USER 조회 범위 "전체"로 수정
- [x] `CLAUDE.md`, `GEMINI.md` — USER 권한 설명 수정
- [x] `.cursor/rules/project-context.mdc` — 프롬프트 조회 권한 테이블 수정

---

### feat/admin-user-usage-list. 관리자용 전체 회원 사용량 목록 조회 API 구현

> 관리자 화면의 사용자 목록에서 전체 회원의 금일 사용량을 단일 API 호출로 조회.
> DB LEFT JOIN + GROUP BY 단일 쿼리로 처리 (N+1 없음).
> `UserUsageRespDto` 재사용 — 별도 DTO 생성 없음.

#### Mapper
- [x] `UserMapper.java` — `selectListWithTodayUsage()` 추가
- [x] `UserMapper.xml` — `"user" LEFT JOIN prompt_history` + `GREATEST(0, ...)` 단일 쿼리 추가
  - `u.id AS user_id` alias → `mapUnderscoreToCamelCase` 자동 매핑
  - 날짜 범위 조건 — 기존 `countTodayByUserId`와 동일한 인덱스 활용

#### Service
- [x] `UserService` — `getUserUsageList()` 시그니처 추가
- [x] `UserServiceImpl` — 구현 추가 (단순 위임)

#### Controller
- [x] `UserController` — `GET /api/users/usage` 엔드포인트 추가 (ADMIN 전용)
  - 정적 경로(`/usage`) 우선 매칭 → 기존 `/{id}/usage`와 충돌 없음

#### 테스트
- [x] `UserServiceTest` — `getUserUsageList` 케이스 4개 추가
  - 생성 회원 포함 여부 / 이력 없는 유저 todayUsedCount=0 / N회 카운트 정확성 / 한도 초과 시 remainingCount=0

---

### feat/user-usage. 사용자 일일 사용량 조회 API 구현

> 사용자 본인 및 ADMIN이 일일 할당량 대비 오늘 사용 횟수 / 남은 횟수를 조회할 수 있는 API 추가.
> 기존 `countTodayByUserId` 쿼리를 인덱스 친화적 범위 조건으로 개선 (일일 한도 체크에도 동시 적용).

#### 쿼리 개선
- [x] `PromptHistoryMapper.xml` — `countTodayByUserId`: `DATE(init_regi_dttm) = CURRENT_DATE` → 범위 조건으로 수정
  - `AND init_regi_dttm >= CURRENT_DATE AND init_regi_dttm < CURRENT_DATE + INTERVAL '1 day'`
  - 사용량 조회 API + 기존 일일 한도 체크 모두에 적용

#### DB 인덱스
- [x] `db/schema.sql` — `idx_prompt_history_user_date` 인덱스 주석 현행화 (이미 존재, 범위 조건 설명으로 업데이트)

#### DTO
- [x] `UserUsageRespDto` — `userId`, `loginId`, `name`, `dailyLimit`, `todayUsedCount`, `remainingCount` + `of(User, int)` 정적 팩토리

#### Service
- [x] `UserService` — `getUserUsage(Long userId)` 시그니처 추가
- [x] `UserServiceImpl` — 구현 + `PromptHistoryMapper` 의존성 주입 (크로스 도메인 — 현 관행 수용)

#### Controller
- [x] `UserController` — 엔드포인트 2개 추가
  - `GET /api/users/me/usage` — 본인 사용량 조회 (모든 인증 사용자, `@PreAuthorize` 없음)
  - `GET /api/users/{id}/usage` — 특정 회원 사용량 조회 (ADMIN 전용)

#### 테스트
- [x] `UserServiceTest` — `getUserUsage` 케이스 4개 추가
  - 오늘 사용 없음 / N회 사용 후 / 한도 초과 시 remainingCount=0 / 미존재 유저 예외

---

### feat/human-prompt-step-attr-option-cud. 인물 프롬프트 Step / Attr / Option CUD 구현

> 인물 이미지 프롬프트 생성 시 선택지로 사용하는 5단계 구조(Step → Attr → Option)에 대해
> CUD(생성·수정·삭제) API를 구현. 기존 TagGroup/Tag CUD 패턴과 동일한 삭제 정책 적용.

#### 삭제 정책 (사용 중 거부)
- Step 삭제 시 하위 Attr 존재하면 거부 (`STEP_HAS_ATTRS`)
- Attr 삭제 시 하위 Option 존재하면 거부 (`ATTR_HAS_OPTIONS`)
- Option 삭제 시 `human_prompt_history_option` 참조 중이면 거부 (`OPTION_IN_USE`)

#### ErrorCode
- [x] `HumanPromptErrorCode` — `STEP_NOT_FOUND`, `STEP_HAS_ATTRS`, `ATTR_NOT_FOUND`, `ATTR_HAS_OPTIONS`, `OPTION_NOT_FOUND`, `OPTION_IN_USE` 추가

#### Mapper 확장
- [x] `HumanPromptStepMapper` — `selectById`, `insert`, `update`, `deleteById` 추가
- [x] `HumanPromptAttrMapper` — `selectById`, `selectList`, `existsByStepId`, `insert`, `update`, `deleteById` 추가
- [x] `HumanPromptOptionMapper` — `selectById`, `selectList`, `existsByAttrId`, `insert`, `update`, `deleteById` 추가
- [x] `HumanPromptHistoryOptionMapper` — `existsByOptionId` 추가

#### DTO
- [x] `HumanPromptStepCreateReqDto`, `HumanPromptStepUpdateReqDto`, `HumanPromptStepSimpleRespDto`
- [x] `HumanPromptAttrCreateReqDto`, `HumanPromptAttrUpdateReqDto`, `HumanPromptAttrSimpleRespDto`
- [x] `HumanPromptOptionCreateReqDto`, `HumanPromptOptionUpdateReqDto`, `HumanPromptOptionSimpleRespDto`

#### Service
- [x] `HumanPromptStepService` / `HumanPromptStepServiceImpl`
- [x] `HumanPromptAttrService` / `HumanPromptAttrServiceImpl`
- [x] `HumanPromptOptionService` / `HumanPromptOptionServiceImpl`

#### Controller
- [x] `HumanPromptController` — Step CUD 엔드포인트 3개 추가
  - `POST /api/human-prompts/steps` — Step 생성 (MANAGER 이상)
  - `PUT /api/human-prompts/steps/{id}` — Step 수정 (MANAGER 이상)
  - `DELETE /api/human-prompts/steps/{id}` — Step 삭제 (MANAGER 이상)
- [x] `HumanPromptAttrController` 신규 생성 (`/api/human-prompts/attrs`)
  - `POST /api/human-prompts/attrs` — Attr 생성 (MANAGER 이상)
  - `PUT /api/human-prompts/attrs/{id}` — Attr 수정 (MANAGER 이상)
  - `DELETE /api/human-prompts/attrs/{id}` — Attr 삭제 (MANAGER 이상)
- [x] `HumanPromptOptionController` 신규 생성 (`/api/human-prompts/options`)
  - `POST /api/human-prompts/options` — Option 생성 (MANAGER 이상)
  - `PUT /api/human-prompts/options/{id}` — Option 수정 (MANAGER 이상)
  - `DELETE /api/human-prompts/options/{id}` — Option 삭제 (MANAGER 이상)

#### 수정 가능 필드
- Step: `stepNumber`, `name` (stepCode는 비즈니스 식별자이므로 수정 불가)
- Attr: `name`, `isMultiSelect`, `isRequired`, `sortOrder`
- Option: `name`, `promptValue`, `sortOrder`

#### 테스트
- [x] `HumanPromptStepServiceTest` — Step CUD 5케이스
- [x] `HumanPromptAttrServiceTest` — Attr CUD 7케이스 (Step 삭제 하위Attr 거부 포함)
- [x] `HumanPromptOptionServiceTest` — Option CUD 8케이스 (Attr 삭제 하위Option 거부 포함)
- [x] `PromptServiceTests` Suite — 3개 신규 테스트 클래스 추가

---

### feat/history-detailize. 이력 조회 상세화

> 이력 조회(`GET /api/prompts`) 응답에 프롬프트 생성 시 입력했던 모든 파라미터를 포함하도록 상세화.
> 상품: 카테고리 전체 경로명, 태그명 목록, 프롬프트 스타일 추가.
> 인물: 선택 옵션명 목록, 생성 목적, 추가 지시사항, 프롬프트 스타일 추가.

#### DB 스키마
- [x] `prompt_history_tag` → `goods_prompt_history_tag` 테이블명 변경
- [x] `prompt_history` 테이블에 `prompt_style VARCHAR(20) NOT NULL`, `purpose TEXT`, `additional_notes TEXT` 컬럼 추가
  - ℹ️ `prompt_style` 기본값 책임은 Java Service 레이어로 부여 (DB DEFAULT 없음)
- [x] `human_prompt_history_option` 신규 테이블 + 인덱스 생성
- [x] `db/schema.sql` 반영, `docs/erd.md` 반영

#### Entity / DTO
- [x] `PromptHistoryTag` → `GoodsPromptHistoryTag` 클래스명 변경
- [x] `PromptHistory` entity — `promptStyle`, `purpose`, `additionalNotes` 필드 추가
- [x] `HumanPromptHistoryOption` entity 신규 생성
- [x] `PromptHistoryRespDto` — `categoryPath`, `tagNames`, `promptStyle`, `purpose`, `additionalNotes`, `optionNames` 필드 추가
  - `from()` → `fromGoods(history, categoryPath, tagIds, tagNames)` / `fromHuman(history, optionNames)` factory 메서드 분리
- [x] `GoodsPromptCreateReqDto` — `promptStyle` 필드 추가 (미입력 시 서비스에서 `NANO_BANANA` 기본값 적용)

#### Mapper
- [x] `PromptHistoryTagMapper` → `GoodsPromptHistoryTagMapper` 이름 변경 (Java + XML)
  - XML 테이블명 `prompt_history_tag` → `goods_prompt_history_tag`
  - `selectTagNamesByPromptHistoryId` 추가 (Tag JOIN)
- [x] `PromptHistoryMapper.xml` — INSERT / SELECT에 `prompt_style`, `purpose`, `additional_notes` 컬럼 추가
- [x] `HumanPromptHistoryOptionMapper` 신규 생성 (`insertBatch`, `selectOptionNamesByHistoryId`)

#### Service
- [x] `PromptServiceImpl.createGoodsPrompt` — `promptStyle` 저장, 응답에 `categoryPath` + `tagNames` 포함
- [x] `HumanPromptServiceImpl.createPrompt` — `promptStyle`, `purpose`, `additionalNotes` 저장, `human_prompt_history_option` 배치 INSERT, 응답에 `optionNames` 포함
- [x] `PromptServiceImpl.getPromptHistories` — PRODUCT/PERSON 분기하여 각 신규 필드 JOIN 조회
- [x] `TagServiceImpl` — `GoodsPromptHistoryTagMapper` 참조로 업데이트

#### 테스트
- [x] `PromptServiceTest` — 상품 신규 필드 검증 4개 (promptStyle MID_JOURNEY / 기본값 / categoryPath / tagNames), PRODUCT 이력 조회 신규 필드 검증
- [x] `HumanPromptServiceTest` — 인물 신규 필드 검증 3개 (optionNames / purpose+additionalNotes / promptStyle MID_JOURNEY), PERSON 이력 조회 신규 필드 검증
- [x] ASIS 테스트 `히스토리_직접_삽입` 헬퍼 4곳에 `.promptStyle("NANO_BANANA")` 추가 (NOT NULL 제약 대응)

---

### 버그 수정 (`fix` 브랜치)

- [x] `TagServiceImpl.deleteTag()` — `goods_prompt_history_tag`에서 참조 중인 태그 삭제 방어 누락 → `TAG_IN_USE` 예외 추가
- [x] `CategoryServiceImpl.deleteCategory()` — `prompt_history`에서 참조 중인 카테고리 삭제 방어 누락 → `CATEGORY_IN_USE` 예외 추가
- [x] `HumanPromptServiceImpl.createPrompt()` — 인물 프롬프트 생성 시 일일 한도 체크 누락 → 수정
- [x] `PromptHistoryMapper.xml updateImageUrl` — `final_rvis_pic` 컬럼 누락 → 수정
- [x] `HumanPromptCreateReqDto` — `@Builder` 어노테이션 누락 → 추가
- [x] `TagServiceTest` — `예외_태그_삭제_사용_중` 테스트 추가
- [x] `CategoryServiceTest` — `예외_카테고리_삭제_사용_중` 테스트 추가
- [x] `UserMapper.xml` — `UserSearchReqDto` 변수 바인딩을 위한 `@Setter` 추가 (PR #33, iamjy)

---

### feat/prompt-image-person. 인물 이미지 프롬프트 + NanoBanana 연동

> 상품 이미지 프롬프트(Tab 1) 외에, 인물 사진 이미지를 위한 별도 프롬프트 생성 플로우 추가.
> NanoBanana(`gemini-3-pro-image-preview`) 직접 연동으로 이미지 생성 및 URL 반환까지 지원.

#### DB 테이블 + 시드 데이터
- [x] `human_prompt_step` — 5단계 구조 (A~E)
- [x] `human_prompt_attr` — 단계별 세부항목
- [x] `human_prompt_option` — 세부항목별 선택지 (`prompt_value` 포함)
- [x] `db/schema.sql` 반영, `docs/erd.md` 반영
- [x] `db/data.sql` — 5단계 시드 데이터 추가

#### Entity / Mapper
- [x] `HumanPromptStep`, `HumanPromptAttr`, `HumanPromptOption` entity
- [x] `HumanPromptStepMapper` + XML — `selectAllWithAttrsAndOptions` (5단계 계층 구조 1회 쿼리 조회)
- [x] `HumanPromptOptionMapper` + XML — `selectByIds`

#### Service
- [x] `HumanPromptService` + `HumanPromptServiceImpl`
  - `getSteps()` — 5단계 구조 반환
  - `createPrompt()` — 선택 옵션 기반 Gemini 호출, 이력 저장 (`@Transactional`)
  - `generateImage()` — NanoBanana API 호출, `imageUrl` 이력 업데이트
- [x] `ImageGenerationService` + `NanoBananaServiceImpl`
  - `gemini-3-pro-image-preview` 모델 호출, 생성 이미지 서버 저장 후 URL 반환
- [x] `GeminiService.generateHumanPrompt()` 추가
  - `stepOptionValues`, `purpose`, `additionalNotes`, `promptStyle` 인자 수신
  - NANO_BANANA / MID_JOURNEY 스타일별 시스템 프롬프트 분기
- [x] `HumanPromptErrorCode` (H001 옵션미선택)
- [x] `ImageGenerationErrorCode`

#### Controller
- [x] `HumanPromptController` (`/api/human-prompts`)
  - `GET /steps` — 단계 구조 조회 (USER 이상)
  - `POST /` — 인물 프롬프트 생성 (USER 이상, `@AuthenticationPrincipal`)
  - `POST /generate-image` — NanoBanana 이미지 생성 (USER 이상)

#### 기타
- [x] `build.gradle` — `bootRun` 시 `.env` 환경변수 자동 주입 설정 추가

#### 테스트
- [x] `HumanPromptServiceTest` 작성 (`@MockBean GeminiService`, `@MockBean ImageGenerationService`)
  - 단계 구조 조회 (5단계 반환 / 세부항목·선택지 포함)
  - 인물 프롬프트 생성 — 정상 / 일일 한도 미만 / 한도 초과 / PRODUCT 이력 합산 / 사용자 미존재 / 선택 옵션 없음
  - 이미지 생성 — imageUrl 이력 업데이트 / historyId null 시 미업데이트
- [x] `PromptServiceTests` Suite — `HumanPromptServiceTest` 포함

---

### Refactor. Goods 네이밍 명시 (`refactor/goods-naming`)

> **배경**: 인물(People) 프롬프트 생성 기능 추가 예정으로, 기존 상품(Goods) 프롬프트 관련
> 메서드·클래스·DB 컬럼에 `Goods`를 명시하여 향후 `People`, `GoodsAndPeople` 구분을 명확히 한다.
> `getPromptHistories()` 조회 메서드는 상품/인물 구분 없이 동일 관점이므로 변경하지 않는다.

#### DTO
- [x] `PromptCreateReqDto` → `GoodsPromptCreateReqDto` (파일명 포함 rename)
  - [x] `categoryId` 필드 → `goodsCategoryId`

#### GeminiService / GeminiServiceImpl
- [x] `GeminiService.generatePrompt()` → `generateGoodsPrompt()`
- [x] `GeminiServiceImpl.generatePrompt()` → `generateGoodsPrompt()`
- [x] `GeminiServiceImpl.SYSTEM_PROMPT` 상수 → `GOODS_SYSTEM_PROMPT`
- [x] `GeminiServiceImpl.buildUserMessage()` → `buildGoodsUserMessage()`

#### PromptService / PromptServiceImpl
- [x] `PromptService.createPrompt()` → `createGoodsPrompt()`
- [x] `PromptServiceImpl.createPrompt()` → `createGoodsPrompt()`

#### PromptController
- [x] `createPrompt()` → `createGoodsPrompt()`
- [x] 엔드포인트 URL `POST /api/prompts` → `POST /api/prompts/goods`
  - ℹ️ `GET /api/prompts` 히스토리 조회는 URL 유지

#### DB 컬럼 + 관련 파일
- [x] `db/schema.sql` — `prompt_history.category_id` → `goods_category_id`
- [x] `PromptHistory` entity — `categoryId` → `goodsCategoryId`
- [x] `PromptHistoryRespDto` — `categoryId` → `goodsCategoryId`
- [x] `PromptHistoryMapper.xml` — SQL 컬럼명 수정
- [x] `docs/erd.md` — `category_id` → `goods_category_id` 반영

#### 테스트 코드
- [x] `PromptServiceTest` — 변경된 메서드명 및 DTO 필드명 반영

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

### Step 4. Security 추가
- [x] `build.gradle` 의존성 추가
  - [x] `spring-boot-starter-security`
  - [x] `jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`
  - [x] `spring-security-test`
- [x] `application.yml` — `jwt.secret` / `jwt.expiration` 설정 추가
- [x] `JwtProperties` (`@ConfigurationProperties(prefix = "jwt")`)
- [x] `common/security/CustomUserDetails` — `UserDetails` 구현체 (User 엔티티 래핑)
- [x] `common/security/CustomUserDetailsService` — `UserDetailsService` 구현 (loginId 기반 조회)
- [x] `common/security/JwtTokenProvider` — 토큰 발급 / 검증 (HMAC-SHA256)
- [x] `common/security/JwtAuthFilter` — `OncePerRequestFilter` (Bearer 토큰 검증 → SecurityContext 등록)
- [x] `common/util/SecurityUtil` — 현재 로그인 사용자 loginId 추출 유틸리티 (미인증 시 "system" 반환)
- [x] `SecurityConfig` — JWT 필터 체인, `RoleHierarchy` (ADMIN > MANAGER > USER), `@EnableMethodSecurity`
  - [x] `PasswordEncoderConfig` 삭제 → `SecurityConfig`로 통합
- [x] 로그인 API (`POST /api/auth/login`) — `AuthController`, `AuthService`, `AuthServiceImpl`
  - [x] `AuthErrorCode` (A001 아이디/비밀번호 불일치)
  - [x] `LoginReqDto` / `LoginRespDto` (accessToken, tokenType, expiresIn)
- [x] 각 Controller `@PreAuthorize` 접근 제어
  - [x] `UserController` — 전체 ADMIN 전용
  - [x] `CategoryController` — GET: USER 이상, CUD: MANAGER 이상
  - [x] `TagGroupController` — GET: USER 이상, CUD: MANAGER 이상
  - [x] `TagController` — GET: USER 이상, CUD: MANAGER 이상
  - [x] `PromptController` — 전체 USER 이상, `@AuthenticationPrincipal` 적용
- [x] `PromptCreateReqDto` — `userId` 필드 제거 (SecurityContext에서 추출)
- [x] `PromptService.createPrompt(Long currentUserId, PromptCreateReqDto dto)` 시그니처 변경
- [x] `UserServiceImpl` / `CategoryServiceImpl` / `TagGroupServiceImpl` / `TagServiceImpl` — `SecurityUtil.getCurrentLoginId()` 적용
- [x] `GlobalExceptionHandler` — `AuthenticationException` (401), `AccessDeniedException` (403) 핸들러 추가
- [x] `SwaggerConfig` — JWT Bearer 인증 스킴 (`bearerAuth`) 등록
- [x] `PromptServiceTest` — `createPrompt(userId, dto)` 시그니처 변경 반영

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](requirements.md)
- 프로젝트 진입점: [`CLAUDE.md`](../CLAUDE.md)

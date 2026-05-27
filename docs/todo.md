# TODO

> 상태: ✅ 진행 중인 브랜치 없음
> 최종 업데이트: 2026-05-27 (feat/sort-order-auto-increment 완료)

---

## 진행 중

없음

---

## 기술 부채 (TODO)

없음

---

## 완료

### feat/sort-order-auto-increment. 태그/인물속성 정렬순서 자동 증가

> 태그·인물속성 생성 시 `sort_order`(및 `step_number`)를 직접 입력하지 않아도
> 현재 스코프 내 최대값 + 1로 자동 세팅하는 기능.

- [x] BO — TagGroup, Tag, HumanPromptAttr, HumanPromptOption: `selectMaxSortOrder` Mapper 추가 + Service 자동 세팅
- [x] BO — HumanPromptStep: `selectMaxStepNumber` + `existsByStepNumber` 추가, stepCode 자동 생성 (stepNumber 기반 알파벳)
- [x] BO — Create DTO: sortOrder/stepNumber/stepCode `@NotNull` 제거 → 선택 사항으로 변경
- [x] BO — `DUPLICATE_STEP_NUMBER` 에러코드 추가
- [x] BO — 서비스 테스트: 각 도메인 자동증가/연속증가 테스트 추가
- [x] FO — TagView: 태그 추가 폼 sortOrder 자동증가 UI 반영 (선택사항 힌트)
- [x] FO — HumanPromptStructureView: Step/Attr/Option 추가 폼 자동증가 UI 반영
- [x] FO — stepCode 입력 필드 숨김 (BO 자동 생성)

---

### refactor/user-controller-security-requirement. UserController 컨벤션 정합 및 테스트 보완

> `UserController`의 `@SecurityRequirement(name = "bearerAuth")` 선언 위치를
> 각 `@Operation(security = ...)` 인라인에서 클래스 레벨 단일 선언으로 변경.
> `UserControllerTest` 접근 권한 케이스 누락분 전수 추가.

- [x] `UserController` — `@SecurityRequirement(name = "bearerAuth")` 클래스 레벨로 이동, 메서드별 인라인 선언 제거
  - `@PreAuthorize("hasRole('ADMIN')")` 는 `GET /me/usage`(USER 권한)가 혼재하므로 메서드 레벨 유지
- [x] `UserControllerTest` — 접근 권한 케이스 보완 (기존 28개 → 42개)
  - `GET /me/usage` — USER 200, MANAGER 200, ADMIN 200, 비로그인 401
  - `/me/usage` 200 테스트: `@WithMockUser` 대신 `.with(user(customUserDetails(...)))` 패턴 적용
    (`@AuthenticationPrincipal CustomUserDetails` 타입 불일치로 NPE 방지)
  - ADMIN 전용 GET 엔드포인트 403 전수 보완: `GET /users`, `GET /users/{id}`, `GET /{id}/usage`, `GET /usage`
  - `POST /api/users` USER 403 추가 (기존에 MANAGER만 있었음)

---

### feat/dto-field-validation. DTO 필드 길이 검증 및 컨트롤러 파라미터 검증 강화

> DB VARCHAR 컬럼 크기에 맞게 모든 Request DTO에 `@Size` 어노테이션을 추가.
> `AuthController`·`NsistEmpMappingController`에 `@Validated`와 파라미터 레벨 제약을 적용하여
> `nsistLogin`(empCd·orgCd), `deleteMapping`(empCd) 입력 검증을 보완.
> 각 컨트롤러 테스트에 `@Size` 초과 케이스를 추가하여 검증 커버리지 확보.

#### DTO — `@Size` 추가

- `LoginReqDto` — `loginId` max=50
- `NsistEmpMappingCreateReqDto` — `empCd` max=6
- `UserCreateReqDto` — `loginId` max=50, `name` max=100, `teamName` max=100
- `UserUpdateReqDto` — `name` max=100, `teamName` max=100
- `CategoryCreate/UpdateReqDto` — `name` max=200
- `TagGroupCreate/UpdateReqDto` — `name` max=100
- `TagCreate/UpdateReqDto` — `name` max=100, `tagValue` max=50
- `HumanPromptStepCreate/UpdateReqDto` — `stepCode` max=2, `name` max=100
- `HumanPromptAttrCreate/UpdateReqDto` — `name` max=100
- `HumanPromptOptionCreate/UpdateReqDto` — `name` max=200, `promptValue` max=500

#### 컨트롤러

- `AuthController` — `@Validated` 추가, `nsistLogin` empCd `@NotBlank @Size(max=6)`, orgCd `@NotBlank`
- `NsistEmpMappingController` — `@Validated` 추가, `deleteMapping` empCd `@NotBlank @Size(max=6)`

#### 테스트

- 각 컨트롤러 테스트에 `@Size` 초과 케이스 추가 (총 15건 신규)
- `AuthControllerTest` — nsist SSO 로그인 파라미터 검증 4건 신규 (empCd 빈값·공백·길이초과, orgCd 빈값)
- `NsistEmpMappingControllerTest` — empCd 길이초과 2건 신규 (등록·삭제)

#### 문서

- `docs/todo.md` 현행화
- `.cursor/rules/coding-convention.mdc` — Request DTO `@Size` 적용 컨벤션 추가

---

### feat/human-prompt-per-person-options. 인물 프롬프트 인원별 개별 옵션 선택

> 인물 프롬프트 생성 시 N명 선택 → 각 인물별로 옵션을 독립적으로 선택할 수 있도록 변경.
> 공통 설정(조명/촬영)과 인물별 개별 설정(피사체 정의)을 분리하는 단계 구조 개편 포함.
> Admin이 단계/속성/선택지를 자유롭게 추가·수정·삭제해도 코드 변경 없이 자동 반영.

#### DB 스키마 변경
- [x] `human_prompt_step` — `is_common BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가
- [x] `human_prompt_history_option` — `person_index INTEGER NOT NULL DEFAULT 0` 컬럼 추가
  - personIndex -1: 공통 옵션 / 0 이상: 인물 인덱스
- [x] `db/schema.sql`, `db/data.sql` 반영
  - 단계 구조 3단계로 개편: A(핵심피사체, 개별), B(조명/배경, 공통), C(촬영기술, 공통)
  - 핵심 피사체 정의 attr 확장: 표정, 시선 처리, 의상 스타일, 헤어스타일, 헤어 컬러, 체형 추가
  - 인원수 attr 제거 (FO 인원수 선택 UI로 대체)

#### Entity 변경
- [x] `HumanPromptStep` — `isCommon` 필드 추가
- [x] `HumanPromptHistoryOption` — `personIndex` 필드 추가
- [x] `HumanPromptOption` — `stepName` 필드 추가 (selectByIds JOIN 조회 시 포함)

#### DTO 변경
- [x] `HumanPromptCreateReqDto` — 구조 전면 개편
  - 제거: `selections`(단일 flat map), `purpose`
  - 추가: `persons(List<PersonSelectionDto>)`, `isGroup(Boolean)`, `commonSelections`, `relationship`
- [x] `PersonSelectionDto` 신규 생성 — 인물 1명의 selections 담는 내부 전용 DTO
- [x] `HumanPromptStepCreateReqDto` / `HumanPromptStepUpdateReqDto` — `isCommon`, `stepCode`(수정 가능) 추가
- [x] `HumanPromptStepSimpleRespDto` — `isCommon` 추가 (`@JsonProperty("isCommon")` 명시)
- [x] `HumanPromptStepRespDto` — `isCommon` 추가 (`@JsonProperty("isCommon")` 명시)
- [x] `PromptHistoryRespDto` — `optionNames` 제거 → `personOptions(List<PersonOptionGroupDto>)` 교체
  - `PersonOptionGroupDto`: personIndex, label("공통"/"사람 1"...), optionNames

#### Mapper 변경
- [x] `HumanPromptOptionMapper.xml` — `selectByIds`에 `s.name AS step_name` 추가
- [x] `HumanPromptHistoryOptionMapper.xml` — `insertBatch`에 `person_index` 추가, `selectOptionDetailsByHistoryIds`에 `person_index` 추가 및 정렬 조건 추가
- [x] `HumanPromptAttrMapper` — `selectRequiredAttrIdsByStepIsCommon(boolean isCommon)` 추가 (공통/개별 step 분리 검증용)
- [x] `HumanPromptStepMapper.xml` — `insert`/`update`에 `is_common`, `step_code` 추가, `selectById`에 `is_common` 추가
- [x] `HumanOptionDetailDto` — `personIndex` 필드 추가

#### Service 변경
- [x] `GeminiService.generateHumanPrompt()` — 시그니처 변경
  - `Map<Integer, List<String>>` → `Map<String, List<String>>` (step_number → step 이름 기반)
  - `isGroup`, `relationship` 파라미터 추가
  - `STEP_LABELS` 하드코딩 제거 → DB step 이름 직접 사용 (Admin 변경 시 자동 반영)
  - `generationConfig(maxOutputTokens=4096)` 추가 (다인원 프롬프트 길이 대응)
- [x] `HumanPromptServiceImpl.createPrompt()` — 전면 개편
  - 공통/개별 step 필수 attr 분리 검증
  - 인물별 옵션 처리 (personsStepValues 구성)
  - personIndex 포함 HumanPromptHistoryOption 배치 저장
- [x] `HumanPromptStepServiceImpl` — `isCommon`, `stepCode` 수정 지원 추가
- [x] `PromptServiceImpl.getPromptHistories()` — PERSON 옵션 그룹핑 로직 변경
  - historyId → personIndex → optionNames 2단계 groupingBy
  - PersonOptionGroupDto 변환 (personIndex 오름차순 정렬, label 생성)

#### FO 변경 (ns-creative-studio-fo)
- [x] `PromptNewView.vue` — 인물 탭 전면 개편
  - 인원수 선택 UI (1~5명 개별 탭 / 6명 이상 공통 패널)
  - 공통 옵션(촬영 환경 설정)과 인물별 옵션 분리 렌더링
  - `commonSelectedOptions`, `personRelationship` 상태 추가
  - API 요청: `persons` 배열 + `commonSelections` + `relationship`
- [x] `PromptHistoryView.vue` — personOptions 기준 인물별 그룹 표시
  - 공통 옵션: teal / 인물별 옵션: deep-purple 색상 구분
- [x] `HumanPromptStructureView.vue` — 인물 속성 관리 UI 개선
  - 단계 목록에 공통/개별 뱃지 표시
  - 단계 추가/수정 다이얼로그에 isCommon 스위치 토글 추가
  - ↑ ↓ 버튼으로 단계 순서 변경 (step_number 스왑)
  - 단계 코드(stepCode) 수정 가능하도록 변경

#### 테스트
- [x] `HumanPromptServiceTest` — 전면 재작성
  - 단계 구조 검증: 특정 코드 하드코딩 제거 → 유연한 검증 (is_common 구분, 단계 존재 여부)
  - 최소 DTO 헬퍼: `공통_최소_선택지_구성()`, `개별_최소_선택지_구성()`, `최소_DTO_구성_1명()`
  - mock stub: `any()` 5개 → 7개 (`anyBoolean()` 포함)
  - 신규 테스트: 2명 + 관계 입력, personOptions 이력 조회 검증
- [x] `HumanPromptControllerTest` — `selections null → 400` 테스트 제거 (더 이상 @NotNull 없음), `stepCode` 관련 검증 추가
- [x] `HumanPromptStepServiceTest` — update DTO에 `stepCode` 추가
- [x] `HumanPromptOptionServiceTest` — insertBatch 시 `personIndex(0)` 명시 추가

---

### refactor/nsist-emp-cd-db. nsist SSO empCd 매핑을 별도 테이블로 전환

> `application-{profile}.yml`의 `nsist.emp-cd-map` 하드코딩 매핑을 제거하고,
> `nsist_emp_mapping` 테이블을 신규 생성하여 DB 기반으로 전환.
> 여러 nsist 사번이 하나의 NCS 팀 계정을 공유하는 운영 환경 요구사항을 반영 (N:1 구조).
> ADMIN이 API로 매핑을 직접 관리(`POST/GET/DELETE /api/nsist-emp-mappings`).

- [x] `nsist_emp_mapping` 테이블 신규 생성 (`db/schema.sql`, DDL, `erd.md`)
- [x] `NsistEmpMapping` 엔티티 생성 (`domain/auth/entity/`)
- [x] `NsistEmpMappingErrorCode` enum 생성 (`domain/auth/`)
- [x] `NsistEmpMappingMapper` 인터페이스 + XML 생성 (`selectByEmpCd`, `selectList`, `insert`, `deleteByEmpCd`)
- [x] `NsistEmpMappingRespDto` / `NsistEmpMappingCreateReqDto` 생성
- [x] `NsistEmpMappingService` 인터페이스 + `NsistEmpMappingServiceImpl` 생성
- [x] `NsistEmpMappingController` 생성 (ADMIN 전용 — `POST/GET/DELETE /api/nsist-emp-mappings`)
- [x] `AuthServiceImpl` — `NsistProperties.empCdMap` 제거 → `NsistEmpMappingMapper.selectByEmpCd()` 기반으로 교체
- [x] `NsistProperties` 클래스 삭제 (역할 소멸)
- [x] `application-local.yml` / `application-dev.yml` — `nsist.emp-cd-map` 블록 제거

---

### refactor/extract-prompt-constants. 이미지 생성 장수 상수 PromptConstants로 통합

> `PromptServiceImpl`에 `private`으로 선언된 `IMAGE_GENERATE_COUNT(3)`를 `PromptConstants` 공개 상수로 격상.
> `HumanPromptServiceImpl`의 하드코딩 리터럴 `3`을 동일 상수로 교체.
> 이제 이미지 생성 수를 변경할 때 `PromptConstants.java` 한 곳만 수정하면 된다.

- [x] `PromptConstants.java` 신규 생성 (`domain/prompt/` 패키지)
- [x] `PromptServiceImpl` — 자체 `private static final IMAGE_GENERATE_COUNT` 선언 제거, `PromptConstants.IMAGE_GENERATE_COUNT` 참조로 교체
- [x] `HumanPromptServiceImpl` — 리터럴 `3` → `PromptConstants.IMAGE_GENERATE_COUNT` 교체

---

### refactor/prompt-type-style-enum. PromptType/PromptStyle enum 도입

> `prompt_history` 테이블의 `prompt_type`, `prompt_style` 컬럼 값을 하드코딩 문자열 대신 enum 타입으로 관리.
> Request DTO에서 Jackson/Spring MVC 자동 변환 + 잘못된 값 입력 시 400 자동 반환.
> `PromptHistory` 엔티티, 응답 DTO는 `String` 유지 (MyBatis 호환).

#### 신규 파일
- [x] `PromptType.java` — `PRODUCT` | `PERSON` enum (domain/prompt 패키지)
- [x] `PromptStyle.java` — `NANO_BANANA` | `MID_JOURNEY` enum (domain/prompt 패키지)

#### Request DTO — enum 타입으로 교체
- [x] `GoodsPromptCreateReqDto.promptStyle` `String` → `PromptStyle`
- [x] `HumanPromptCreateReqDto.promptStyle` `String` → `PromptStyle`
- [x] `PromptHistorySearchReqDto.promptType` `String` → `PromptType`
- [x] `PromptHistorySearchReqDto.promptStyle` `String` → `PromptStyle`

#### GeminiService / GeminiServiceImpl — 내부 서비스 파라미터 enum 적용
- [x] `GeminiService` — `generateGoodsPrompt`, `generateHumanPrompt` 파라미터 `String promptStyle` → `PromptStyle promptStyle`
- [x] `GeminiServiceImpl` — 동일 시그니처 변경 + `"MID_JOURNEY".equalsIgnoreCase(promptStyle)` → `PromptStyle.MID_JOURNEY == promptStyle`

#### Service — enum 직접 참조 + entity builder `.name()` 호출
- [x] `PromptServiceImpl` — 로컬 변수 `String promptStyle` → `PromptStyle promptStyle`, Gemini 호출 시 enum 직접 전달, entity builder `.promptStyle(promptStyle.name())`
- [x] `HumanPromptServiceImpl` — 동일 (`style` 변수)

#### Mapper XML
- [x] `PromptHistoryMapper.xml` — `<if test="promptType != null and promptType != ''">` → `<if test="promptType != null">` (enum과 빈 문자열 비교 제거)
- [x] `PromptHistoryMapper.xml` — `<if test="promptStyle != null and promptStyle != ''">` → `<if test="promptStyle != null">` (동일)

#### 테스트
- [x] `PromptServiceTest` — 헬퍼 `히스토리_직접_삽입_상세` 시그니처 `PromptType/PromptStyle` enum으로 교체, 13개 호출부 enum 상수로, 6개 어설션/필터 `.name()` 비교로
- [x] `HumanPromptServiceTest` — `PromptType` import 추가, entity builder 2곳 `.name()` 호출, 어설션 3곳 enum `.name()` 비교로
- [x] `CategoryServiceTest` — `PromptType/PromptStyle` import 추가, entity builder `.name()` 호출
- [x] `UserServiceTest` — `PromptType/PromptStyle` import 추가, entity builder `.name()` 호출
- [x] `TagServiceTest` — `PromptType/PromptStyle` import 추가, entity builder `.name()` 호출

---

### test/human-prompt-db-independence. HumanPrompt 테스트 DB 의존성 약화

> DB 상태(수동 insert 등)에 따라 간헐적으로 실패하던 테스트 4건을 개선.
> 시드 데이터 기반 어서션으로 전환하고, 동적 식별자를 도입하여 DB 잔류 데이터의 영향을 제거.

#### HumanPromptServiceTest
- [x] `성공_단계_구조_조회_5단계_반환` — `hasSize(5)` → `size() >= 5` + `containsSequence(A~E tuple)`
- [x] `성공_단계_구조_조회_세부항목_선택지_포함` — 전체 step 순회 → A~E 시드 step 필터링 후 검증

#### HumanPromptStepServiceTest
- [x] `성공_Step_생성` / `성공_Step_수정_stepNumber_name_변경` / `성공_Step_삭제_하위Attr_없음`
  — 고정 stepCode(`F`/`G`/`H`) → `CODE_SUFFIX`(UUID 1자 hex) 기반 동적 값으로 교체
  — step name도 `RUN_ID` suffix로 고유화

---

### feat/admin-delete-history. 관리자 프롬프트 이력 삭제

> 관리자(ADMIN)가 특정 프롬프트 이력을 삭제할 수 있는 API 구현.
> DB 자식 레코드(매핑 테이블) → 부모 레코드(`prompt_history`) 순서로 삭제하며,
> `imageUrl` 파싱 후 디스크에 저장된 이미지 파일도 함께 삭제한다.

#### ErrorCode
- [x] `PromptErrorCode` — `HISTORY_NOT_FOUND (P007)` 추가

#### Mapper 인터페이스 + XML
- [x] `GoodsPromptHistoryTagMapper` — `deleteByPromptHistoryId` 추가
- [x] `GoodsPromptHistoryTagMapper.xml` — DELETE SQL 추가
- [x] `HumanPromptHistoryOptionMapper` — `deleteByPromptHistoryId` 추가
- [x] `HumanPromptHistoryOptionMapper.xml` — DELETE SQL 추가
- [x] `PromptHistoryMapper` — `deleteById` 추가
- [x] `PromptHistoryMapper.xml` — DELETE SQL 추가

#### Service
- [x] `PromptService` — `deleteHistory(Long id)` 시그니처 추가
- [x] `PromptServiceImpl` — 구현 (`@Transactional`)
  - `selectById` → 미존재 시 `HISTORY_NOT_FOUND (P007)` 예외
  - `PRODUCT` → `goods_prompt_history_tag` 삭제
  - `PERSON`  → `human_prompt_history_option` 삭제
  - `prompt_history` 삭제
  - `imageUrl` 파싱 후 디스크 이미지 파일 삭제 (`image.storage-path` 활용, null/빈값 스킵)
    - JSON 배열 → 다수 파일 삭제 (NanoBanana 3개)
    - 단일 상대 경로 → 파일 1개 삭제

#### Controller
- [x] `PromptController` — `DELETE /api/prompts/{id}` 추가 (ADMIN 전용, `@PreAuthorize("hasRole('ADMIN')")`)

#### 테스트
- [x] `PromptServiceTest` — 삭제 케이스 3개 추가
  - `[성공] 이력 삭제 - PRODUCT 타입 (goods_prompt_history_tag → prompt_history 순 삭제)`
  - `[성공] 이력 삭제 - PERSON 타입 (human_prompt_history_option → prompt_history 순 삭제)`
  - `[예외] 이력 삭제 - 존재하지 않는 id`

---

### refactor/controller-validation. @RestController Validation 누락 보정

> `@PathVariable Long id`에 `@Positive` 적용, `@RequestBody` 누락 `@Valid` 보정, 클래스 레벨 `@Validated` 추가.
> `@Validated` 없이는 `@Positive` 등 파라미터 제약 어노테이션이 AOP로 동작하지 않음.

#### 변경 파일 — Controller
- `CategoryController` — `@Validated`, `@Positive`(3곳), `@Valid` 추가(updateCategory)
- `TagController` — `@Validated`, `@Positive`(3곳), `@Valid` 추가(updateTag)
- `TagGroupController` — `@Validated`, `@Positive`(3곳), `@Valid` 추가(updateTagGroup)
- `HumanPromptAttrController` — `@Validated`, `@Positive`(2곳)
- `HumanPromptOptionController` — `@Validated`, `@Positive`(2곳)
- `HumanPromptController` — `@Validated`, `@Positive`(2곳), `@Valid` 추가(updateStep)
- `UserController` — `@Validated`, `@Positive`(4곳)

#### 변경 파일 — Update ReqDto (`@Valid` 추가로 인한 후속 작업)
- `CategoryUpdateReqDto` — `name` `@NotBlank`, `sortOrder` `@NotNull`
- `TagUpdateReqDto` — `name` `@NotBlank`, `sortOrder` `@NotNull`
- `TagGroupUpdateReqDto` — `name` `@NotBlank`, `isMultiSelect` `@NotNull` (sortOrder는 화면 미지원으로 null 허용)
- `HumanPromptStepUpdateReqDto` — `stepNumber` `@NotNull`, `name` `@NotBlank`

---

### perf/optimize-prompt-history-batch-query. 프롬프트 이력 목록 조회 N+1 쿼리 최적화

> `GET /api/prompts` 이력 목록 조회 시 페이지 크기 N에 비례하여 1+3N 쿼리가 발생하던 문제를 최대 4쿼리(flat)로 개선.
> PageHelper와 호환되는 Batch IN 쿼리 패턴 적용.

#### 변경 전 (AS-IS)
```
쿼리 1: promptHistoryMapper.selectList()                    — 페이지 조회
PRODUCT 1건당:
  쿼리 +1: categoryMapper.selectCategoryPath()             — WITH RECURSIVE CTE (건당 1회)
  쿼리 +1: selectTagIdsByPromptHistoryId()
  쿼리 +1: selectTagNamesByPromptHistoryId()
HUMAN 1건당:
  쿼리 +1: selectOptionNamesByHistoryId()
→ pageSize=N(PRODUCT 기준) 시 1+3N 쿼리 발생
```

#### 변경 후 (TO-BE)
```
쿼리 1: promptHistoryMapper.selectList()                    — 페이지 조회
쿼리 2: categoryMapper.selectCategoryPathsByIds(IN)         — PRODUCT 카테고리 배치 (3-self-JOIN)
쿼리 3: selectTagDetailsByPromptHistoryIds(IN)              — PRODUCT 태그 상세 배치 (tagId + tagName 통합)
쿼리 4: selectOptionDetailsByHistoryIds(IN)                 — HUMAN 옵션명 배치
→ 최대 4쿼리 (N에 무관, PRODUCT/HUMAN 혼재 시 타입별 empty check로 불필요 쿼리 스킵)
```

#### 신규 DTO
- [x] `GoodsTagDetailDto` — goods 태그 배치 조회 결과 (promptHistoryId + tagId + tagName)
- [x] `HumanOptionDetailDto` — human 옵션 배치 조회 결과 (promptHistoryId + optionName)
- [x] `CategoryPathDto` — 카테고리 경로 배치 조회 결과 (id + categoryPath)

#### Mapper 인터페이스 + XML (기존 단건 메서드 유지 — createGoodsPrompt 응답 조립에서 사용 중)
- [x] `GoodsPromptHistoryTagMapper` — `selectTagDetailsByPromptHistoryIds(ids)` 추가
- [x] `HumanPromptHistoryOptionMapper` — `selectOptionDetailsByHistoryIds(ids)` 추가
- [x] `CategoryMapper` — `selectCategoryPathsByIds(ids)` 추가 (3단계 자기 JOIN — SCS 보장 전제)

#### Service
- [x] `PromptServiceImpl.getPromptHistories()` — 배치 쿼리 + `Collectors.groupingBy` 조립으로 전면 교체

---

### feat/prompt-history-filter. 프롬프트 이력 조회 검색 필터 추가

> `GET /api/prompts` 이력 조회에 조건 기반 필터링 추가.
> 기존에는 전체 이력을 페이징만 제공하였으며, 이번 작업으로 아래 조건으로 필터링 가능.
> 모든 필터 조건은 선택 사항 — null이면 전체 조회와 동일.

#### 변경 사항

- [x] `PromptHistorySearchReqDto` 신규 생성 (`PageReqDto` extend, 검색 조건 통합)
  - `loginId` : 생성자 필터 (`init_regi_pic` 컬럼 직접 비교, JOIN 없음)
  - `promptType` : 상품/인물 구분 (`PRODUCT` | `PERSON`)
  - `promptStyle` : 프롬프트 스타일 (`NANO_BANANA` | `MID_JOURNEY`)
  - `dateFrom` / `dateTo` : 생성일 범위 (포함)
- [x] `PromptHistoryMapper` — `selectList()` → `selectList(PromptHistorySearchReqDto dto)` 시그니처 변경
- [x] `PromptHistoryMapper.xml` — `<where>` + `<if>` 동적 SQL 추가 (인라인 CDATA 처리)
- [x] `PromptService` — `getPromptHistories(PageReqDto)` → `getPromptHistories(PromptHistorySearchReqDto)` 시그니처 변경
- [x] `PromptServiceImpl` — `pagedQuery` 호출 방식 변경 (람다로 dto 전달)
- [x] `PromptController` — 파라미터 타입 변경, `@Operation` description 업데이트

#### 테스트

- [x] `PromptServiceTest` — 기존 `getPromptHistories(new PageReqDto(...))` 호출 전체 수정
- [x] `PromptServiceTest` — 필터 신규 케이스 8개 추가
  - `[성공] 히스토리 조회 - loginId 필터`
  - `[성공] 히스토리 조회 - promptType PRODUCT 필터`
  - `[성공] 히스토리 조회 - promptType PERSON 필터`
  - `[성공] 히스토리 조회 - promptStyle MID_JOURNEY 필터`
  - `[성공] 히스토리 조회 - 날짜 범위 필터 (오늘 날짜 포함)`
  - `[성공] 히스토리 조회 - 날짜 범위 필터 (미래 날짜 → 빈 결과)`
  - `[성공] 히스토리 조회 - 복합 필터 (loginId + promptType + promptStyle)`
  - `[없음] 히스토리 조회 - 존재하지 않는 loginId 조건`
- [x] `HumanPromptServiceTest` — `getPromptHistories(new PageReqDto(...))` 호출 수정

---

### fix/sort-order-duplicate-validation. 카테고리/태그 정렬순서 중복 검증 추가

> `sort_order`는 동일 스코프(같은 부모 카테고리 / 같은 TagGroup) 내에서 중복 불가.
> 다른 스코프에서는 동일 `sort_order` 허용. 기존 코드에 중복 검증 로직이 누락되어 있었음.

#### 추가된 ErrorCode
- [x] `CategoryErrorCode.DUPLICATE_SORT_ORDER` (C006) — 동일 부모(parent_id) 스코프 내 중복
- [x] `TagGroupErrorCode.DUPLICATE_SORT_ORDER` (TG003) — 전역 스코프 내 중복
- [x] `TagErrorCode.DUPLICATE_SORT_ORDER` (T004) — 동일 tag_group_id 스코프 내 중복

#### Mapper 인터페이스 + XML
- [x] `CategoryMapper` — `existsBySortOrder(parentId, sortOrder)`, `existsBySortOrderExcludingId(id, parentId, sortOrder)` 추가
- [x] `TagGroupMapper` — `existsBySortOrder(sortOrder)`, `existsBySortOrderExcludingId(id, sortOrder)` 추가
- [x] `TagMapper` — `existsBySortOrder(tagGroupId, sortOrder)`, `existsBySortOrderExcludingId(id, tagGroupId, sortOrder)` 추가
- [x] `CategoryMapper.xml` / `TagGroupMapper.xml` / `TagMapper.xml` — INSERT `sort_order` null 시 컬럼 제외 (`<if test="sortOrder != null">`) → DB `DEFAULT 0` 활용

#### Service
- [x] `CategoryServiceImpl.createCategory` / `updateCategory` — 중복 검증 추가
- [x] `TagGroupServiceImpl.createTagGroup` / `updateTagGroup` — 중복 검증 추가
- [x] `TagServiceImpl.createTag` / `updateTag` — 중복 검증 추가

#### 테스트
- [x] `CategoryServiceTest` — sortOrder 중복 예외 3케이스, 자신의 sortOrder 수정 허용, null 다수 허용
- [x] `TagGroupServiceTest` — sortOrder 중복 예외 2케이스, 자신의 sortOrder 수정 허용, **null 다수 허용 케이스 추가**
- [x] `TagServiceTest` — sortOrder 중복 예외 2케이스, 자신의 sortOrder 수정 허용, 다른 그룹 동일 sortOrder 허용
- [x] 기존 테스트 픽스처 sort_order 1000+ 범위로 변경 (시드 데이터 1~4, 99와 충돌 방지)
  - `CategoryServiceTest`, `TagGroupServiceTest`, `TagServiceTest`, `PromptServiceTest`

#### 문서
- [x] `docs/requirements.md` — Category / TagGroup / Tag `sort_order` 컬럼에 스코프 제약 설명 추가

#### 버그 픽스 (커밋 후 발견)
- [x] `TagGroupServiceImpl` — `sortOrder null` 시 `0`으로 치환 후 중복 체크하던 로직 제거
  - Category / Tag와 동일하게 `null`이면 체크 스킵, DB `DEFAULT 0` 활용
  - 원인: UI에 sortOrder 입력 필드 없어 항상 `null`로 전달 → 두 번째 태그 그룹 생성 시 "이미 정렬순서 0이 존재합니다" 에러
  - `TagGroupServiceTest.성공_태그그룹_생성_sortOrder_null_다수` 테스트 케이스 추가

---

### fix/image-upload-validation. 참조 이미지 업로드 서버 검증 추가

> `POST /api/prompts/goods/generate-image`의 `referenceImage` 파라미터에 대해 서버 측 검증 추가.
> 프론트엔드 검증만으로는 Content-Type 헤더 조작을 막을 수 없으므로 백엔드에서도 검증.

#### 변경 사항
- [x] `ImageGenerationErrorCode` — `UNSUPPORTED_IMAGE_TYPE` (IG005), `IMAGE_TOO_LARGE` (IG006) 추가
- [x] `PromptServiceImpl` — 참조 이미지 검증 로직 추가 (getBytes() 호출 전 순서 보장)
  - 파일 크기 10MB 초과 시 `IMAGE_TOO_LARGE` (IG006) 예외
  - MIME 타입 null 시 `INVALID_IMAGE_MIME_TYPE` (IG004) 예외 (기존)
  - MIME 타입 allowlist(jpeg/png/webp) 외 타입 시 `UNSUPPORTED_IMAGE_TYPE` (IG005) 예외
- [x] `PromptController` — `@ApiResponse(400)` 설명에 MIME 타입 / 파일 크기 조건 추가
- [x] `application.yml` — `spring.servlet.multipart.max-file-size/max-request-size: 10MB` 명시
  - Spring Boot 기본값(1MB)과 코드 상의 10MB 제한 불일치 해소
- [x] `GlobalExceptionHandler` — `MaxUploadSizeExceededException` 핸들러 추가 (400 반환)
  - Spring이 multipart 제한을 먼저 차단할 때도 `CommonResponse` 형식으로 일관된 응답 보장

#### 테스트
- [x] `PromptServiceTest` — 참조 이미지 검증 실패 케이스 4개 추가
  - `[예외] 상품 이미지 생성 - 허용되지 않는 MIME 타입 (text/plain)`
  - `[예외] 상품 이미지 생성 - 허용되지 않는 MIME 타입 (application/x-sh)`
  - `[예외] 상품 이미지 생성 - Content-Type null`
  - `[예외] 상품 이미지 생성 - 파일 크기 10MB 초과`

---

### feat/increase-multipart-file-size-limit. 참조 이미지 업로드 최대 용량 15MB로 상향

> 요구사항 변경: 참조 이미지 파일 크기 제한을 10MB → 15MB로 확대.

#### 변경 사항
- [x] `application.yml` — `spring.servlet.multipart.max-file-size/max-request-size: 15MB`
- [x] `PromptServiceImpl` — `MAX_REFERENCE_IMAGE_BYTES` 상수 `10MB → 15MB`
- [x] `ImageGenerationErrorCode` — `IMAGE_TOO_LARGE` (IG006) 에러 메시지 `최대 10MB → 최대 15MB`
- [x] `GlobalExceptionHandler` — `MaxUploadSizeExceededException` 핸들러 메시지 `최대 10MB → 최대 15MB`
- [x] `PromptController` — `@ApiResponse(400)` 설명 파일 크기 조건 `10MB → 15MB`

#### 테스트
- [x] `PromptServiceTest` — `[예외] 상품 이미지 생성 - 파일 크기 10MB 초과` → `15MB 초과` 로 수정
  - 초과 케이스 파일 크기 `11MB → 16MB` 로 수정

---

### chore/swagger-profile-restriction. Swagger local/dev 프로필 전용으로 제한

> prd 환경에서 Swagger UI 노출을 차단하기 위해 프로필 제한 적용.
> SwaggerConfig Bean 등록 차단 + springdoc 엔드포인트 비활성화 이중 방어(Defense in Depth).

#### 변경 사항
- [x] `SwaggerConfig.java` — `@Profile({"local", "dev"})` 추가 (prd에서 OpenAPI Bean 미등록)
- [x] `application-prd.yml` 신규 생성
  - `springdoc.api-docs.enabled: false`
  - `springdoc.swagger-ui.enabled: false`

---

### chore/cors-local-profile. CorsConfig local 프로필 전용으로 제한

> dev/prod 환경은 APIM에서 CORS를 처리하므로 Spring CORS 설정을 local 프로필로 국한.
> 기존 `CorsConfig`에 `@Profile("local")` 적용.

#### 변경 사항
- [x] `CorsConfig.java` — `@Profile("local")` 추가 (local 프로필에서만 Bean 등록)

---

### refactor/image-url-store-relative-path. DB에 이미지 URL 상대 경로로 저장, 응답 시 절대 URL 조립

> `image.base-url` 설정을 기준으로 응답 시 절대 URL을 조립하는 방식으로 변경.
> DB에는 `/images/{uuid}.ext` 형태의 상대 경로만 저장하고,
> `PromptServiceImpl` / `HumanPromptServiceImpl`에서 응답 생성 시 base-url을 prefix로 붙인다.

#### 변경 사항
- [x] `NanoBananaServiceImpl.saveImage()` — 상대 경로(`/images/...`)만 반환하도록 수정
- [x] `PromptServiceImpl` — `getPromptHistories` 에서 imageBaseUrl 전달
- [x] `HumanPromptServiceImpl` — `getPromptHistories` 에서 imageBaseUrl 전달
- [x] `PromptHistoryRespDto.parseImageUrls()` — `baseUrl` prefix 조립 로직 추가
  - `null/blank` → 빈 List
  - `"[...]"` (JSON 배열) → Jackson 파싱 후 각 경로에 baseUrl prefix
  - 일반 상대 경로 → 단일 원소 List (baseUrl prefix)
  - 이미 `http`로 시작하는 URL → prefix 미적용

---

### fix/image-url-absolute-path. 이미지 URL BO 도메인 기반 절대 경로 반환 (중간 fix)

> `image.base-url` 설정 추가 및 NanoBananaServiceImpl에서 절대 URL 반환하도록 수정.
> 이후 `refactor/image-url-store-relative-path`에서 상대 경로 저장 방식으로 재리팩터링.

#### 변경 사항
- [x] `application.yml` — `image.base-url: http://localhost:8080` 추가
- [x] `application-dev.yml` — `image.base-url: https://nsm-dev-creative-bo.nsmall.com` 추가

---

### chore/cors-config. CORS 설정 추가

> 프론트엔드(FO)와의 크로스 도메인 요청을 허용하기 위한 CORS 설정 추가.
> local 프로필에서만 Spring Boot 수준 CORS를 활성화하고, dev/prod는 APIM에서 처리.

#### 신규 클래스
- [x] `CorsConfig.java` — `@Profile("local")`, `CorsConfigurationSource` Bean 등록
  - 허용 메서드: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`
  - 허용 헤더: `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`
  - `allowCredentials: true` (JWT 전달 허용)
- [x] `CorsProperties.java` — `@ConfigurationProperties(prefix = "cors")`, `allowedOrigins` 바인딩

#### 설정 파일 변경
- [x] `application.yml` — `cors.allowed-origins: [http://localhost:3000]`
- [x] `application-dev.yml` — `cors.allowed-origins: [https://nsm-dev-creative-fo.nsmall.com]`
- [x] `SecurityConfig.java` — `.cors(withDefaults())` 적용

---

### feat/tag-image-size. 프롬프트 이미지 비율(imageAspectRatio) 지원

> 상품 및 인물 프롬프트 생성 시 이미지 출력 비율을 선택할 수 있도록 지원.
> 선택된 비율에 따라 Gemini 호출 후 Java에서 format suffix를 프롬프트에 직접 주입.

#### DB 스키마
- [x] `prompt_history` 테이블에 `image_aspect_ratio VARCHAR(10)` 컬럼 추가
- [x] `db/schema.sql` 반영, `docs/erd.md` 반영

#### 초기 데이터
- [x] `db/data.sql` — 이미지 사이즈 태그 그룹 및 태그 4종 추가
  - `1:1` (Square), `4:3` (Landscape), `16:9` (Landscape wide), `9:16` (Portrait)

#### Entity / DTO
- [x] `PromptHistory` entity — `imageAspectRatio` 필드 추가
- [x] `GoodsPromptCreateReqDto` — `imageAspectRatio` 필드 추가 (선택, 예: `"1:1"`, `"4:3"`)
- [x] `HumanPromptCreateReqDto` — `imageAspectRatio` 필드 추가
- [x] `PromptHistoryRespDto` — `imageAspectRatio` 필드 추가 (fromGoods / fromHuman 공통)

#### Service / GeminiServiceImpl
- [x] `GeminiService.generateGoodsPrompt()` — `imageAspectRatio` 파라미터 추가
- [x] `GeminiService.generateHumanPrompt()` — `imageAspectRatio` 파라미터 추가
- [x] `GeminiServiceImpl` — Gemini 응답 후 Java에서 format suffix 직접 주입
  - `1:1` → `Square format.`
  - `4:3`, `16:9` → `Landscape format.`
  - `9:16` → `Portrait format.`
  - 미입력 시 suffix 없음 (Gemini 기본 비율 사용)
- [x] `GeminiServiceImpl.SYSTEM_PROMPT_PERSON` — `"Default to Portrait format."` 제거 (suffix 주입으로 대체)
- [x] `GeminiServiceImpl` — SUBJECT 섹션 그룹 처리 추가 (Couple/Family/Small group 선택 시 그룹 묘사 유도)
- [x] `PromptServiceImpl` — `imageAspectRatio` Gemini 호출 및 저장에 전달
- [x] `HumanPromptServiceImpl` — `imageAspectRatio` Gemini 호출 및 저장에 전달

#### Mapper XML
- [x] `PromptHistoryMapper.xml` — INSERT / SELECT에 `image_aspect_ratio` 컬럼 추가

#### 테스트
- [x] `PromptServiceTest` — `generateGoodsPrompt` mock 인자 수정 (5파라미터)
- [x] `HumanPromptServiceTest` — `generateHumanPrompt` mock 5개 `any()`로 수정

---

### refactor/goods-prompt-quality. 참조 이미지 기반 이미지 생성 프롬프트 품질 개선

> 참조 이미지가 있을 때 Gemini에 전달하는 지시문을 구체화하여 생성 품질 향상.
> 형태·비율·질감·색감 보존 지시문을 번호 열거 방식으로 명확히 구조화.
> 조명·구도는 promptText에 위임하여 3장 이미지 다양성을 확보.

#### 변경 사항
- [x] `PromptServiceImpl` — 참조 이미지 포함 Gemini 호출 시 지시문 구체화

---

### chore/add-dockerfile. Dockerfile 추가

> GCP Cloud Run 배포를 위한 Dockerfile 추가 (Spring Boot 실행 이미지).

#### 변경 사항
- [x] `Dockerfile` — `eclipse-temurin:17-jre` 기반, `app.jar` 실행

---

### chore/application-dev. dev 프로필 DB 스키마 설정 및 application-dev.yml 추가

> dev 환경 전용 Spring 프로필 설정 파일 추가.

#### 변경 사항
- [x] `application-dev.yml` 신규 생성
  - datasource: `${POSTGRES_HOST}`, `${POSTGRES_PORT}`, `${POSTGRES_DB}`, `${POSTGRES_USER}`, `${POSTGRES_PASSWORD}` 환경변수 참조
  - `cors.allowed-origins: [https://nsm-dev-creative-fo.nsmall.com]`
  - `image.base-url: https://nsm-dev-creative-bo.nsmall.com`

---

### docs/mockitobean-migration. @MockBean → @MockitoBean 마이그레이션 문서화

> 커밋 `03c053e3`에서 `@MockBean` → `@MockitoBean`으로 코드 변경이 이루어졌으나 문서화가 누락됨.
> Spring Boot 3.4+에서 `@MockBean` (deprecated) → `@MockitoBean` 마이그레이션에 따른 주석 현행화.

#### JavaDoc 주석 수정
- [x] `PromptServiceTest.java` — `@MockBean으로 모킹` → `@MockitoBean으로 모킹`
- [x] `HumanPromptServiceTest.java` — `@MockBean으로 모킹` → `@MockitoBean으로 모킹`

#### docs/todo.md 현행화
- [x] `Step 3-D-T` 항목 — `@MockBean GeminiService` → `@MockitoBean GeminiService`
- [x] `feat/prompt-image-person` 항목 — `@MockBean GeminiService`, `@MockBean ImageGenerationService` → `@MockitoBean`

---

### feat/category-depth-3. 상품 카테고리 4단계 → 3단계 변경

> `DCS(세분류)` 제거 → `LCS(대)/MCS(중)/SCS(소)` 3단계 유지.
> SCS가 최종 선택 단계 (기존 DCS 역할 흡수).

#### Java 소스 — 로직 변경
- [x] `CategoryType.java` — `DCS` enum 값 제거
- [x] `CategoryServiceImpl.java` — `EXPECTED_PARENT_TYPE` map에서 `DCS → SCS` 항목 제거
- [x] `PromptServiceImpl.java` — DCS 검증 → SCS 검증 + 인라인 주석 수정
- [x] `PromptErrorCode.java` — `INVALID_CATEGORY_TYPE` 메시지 수정

#### Java 소스 — JavaDoc / 인라인 주석 수정
- [x] `Category.java` — 클래스 JavaDoc: "4단계 계층 구조" → "3단계 계층 구조"
- [x] `CategoryCreateReqDto.java` — JavaDoc: "MCS/SCS/DCS" → "MCS/SCS"
- [x] `GoodsPromptCreateReqDto.java` — JavaDoc: "DCS 카테고리 ID" → "SCS 카테고리 ID"
- [x] `GeminiService.java` — `@param goodsCategoryName`: "LCS→DCS" → "LCS→SCS"
- [x] `CategoryMapper.java` — `selectCategoryPath` JavaDoc 수정
- [x] `PromptHistoryRespDto.java` — `categoryPath` 인라인 주석 3단계 예시로 변경

#### MyBatis XML
- [x] `CategoryMapper.xml` — `selectCategoryPath` 주석 수정 (SQL 변경 없음)

#### DB 스키마
- [x] `db/schema.sql` — category 테이블 주석 수정 (4단계→3단계)

#### 테스트 코드
- [x] `CategoryServiceTest.java` — 3단계 계층 테스트로 전환, DCS 참조 전체 제거
- [x] `PromptServiceTest.java` — `DCS_카테고리_생성()` → `SCS_카테고리_생성()` (3단계), DCS 참조 전체 제거

#### 문서 및 룰
- [x] `docs/requirements.md` — 카테고리 3단계로 수정
- [x] `docs/erd.md` — 계층 구조 예시, 컬럼 설명 수정
- [x] `CLAUDE.md` — 카테고리 설명 수정
- [x] `GEMINI.md` — 카테고리 설명 수정
- [x] `.cursor/rules/project-context.mdc` — 카테고리 계층 설명 수정
- [x] `.cursor/rules/coding-convention.mdc` — `CategoryType` enum 예시에서 `DCS` 제거

---

### feat/paging-prompt-history. 프롬프트 히스토리 조회 페이징 적용

> `GET /api/prompts` 히스토리 조회에 PageHelper 기반 페이징 도입.
> 공통 페이징 모듈(`PageReqDto`, `PageRespDto`, `PageHelperUtil`)을 `common/page/`에 신규 생성.
> `pageNum`(기본 1) / `pageSize`(기본 10) 쿼리 파라미터로 제어.

#### 의존성
- [x] `build.gradle` — `pagehelper-spring-boot-starter:2.1.0` 추가

#### 공통 모듈 (`common/page/`)
- [x] `PageReqDto` — 페이징 요청 DTO (`pageNum`, `pageSize`, 유효성 검증 포함)
- [x] `PageRespDto` — 페이징 응답 DTO (`record`, `of` / `ofWithItems` 팩토리)
  - `ofWithItems` — 쿼리 결과와 응답 타입이 다를 때(추가 조회 후 변환) 메타정보 보존용
- [x] `PageHelperUtil` — `pagedQuery` / `pagedResponse` 정적 유틸 (try-with-resources로 ThreadLocal 안전 처리)

#### Service / Controller
- [x] `PromptService` — `getPromptHistories()` → `getPromptHistories(PageReqDto)` 시그니처 변경
- [x] `PromptServiceImpl` — `pagedQuery` + `ofWithItems` 로 구현 변경
- [x] `PromptController` — `@ModelAttribute @Valid PageReqDto` 추가, 반환 타입 `PageRespDto<PromptHistoryRespDto>`

#### 테스트
- [x] `PromptServiceTest` — 기존 `getPromptHistories()` 호출 4곳 수정 (`.items()` 접근으로 변경)
- [x] `PromptServiceTest` — 페이징 신규 케이스 3개 추가
  - `[성공] 히스토리 조회 페이징 - 첫 페이지 메타정보 검증`
  - `[성공] 히스토리 조회 페이징 - pageSize 제한 적용 확인`
  - `[성공] 히스토리 조회 페이징 - 빈 결과`
- [x] `HumanPromptServiceTest` — `getPromptHistories()` 호출 1곳 수정

---

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
- [x] `HumanPromptServiceTest` 작성 (`@MockitoBean GeminiService`, `@MockitoBean ImageGenerationService`)
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
- [x] `PromptServiceTest` 작성 (`@MockitoBean GeminiService` 모킹)
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

---

### chore/gemini-model. NanoBanana 이미지 생성 모델 교체 (2026-04-22)

> Gemini AI Studio 요청 폭증 대응 — `gemini-3-pro-image-preview` → `gemini-3.1-flash-image-preview`
> `NanoBananaServiceImpl`이 상품·인물 이미지 생성을 공통으로 담당하므로 두 기능 모두 영향.
> 안정화 후 모델 재검토 예정.

- [x] `application.yml` — `gemini.image-model` 값 변경 (상품·인물 이미지 생성 공통 적용)
- [x] 연관 문서 모델명 일괄 업데이트 (`CLAUDE.md`, `GEMINI.md`, `.cursor/rules/project-context.mdc`, `docs/nsm-aimage-architecture.md`)
- [x] 소스 내 주석/JavaDoc/Swagger description 모델명 업데이트 (`GeminiProperties`, `NanoBananaServiceImpl`, `PromptController`, `HumanPromptController`)

---

### chore/change-image-model-3-pro. NanoBanana 이미지 생성 모델 롤백 (2026-04-22)

> Gemini AI Studio 상황 변경에 따라 `gemini-3.1-flash-image-preview` → `gemini-3-pro-image-preview` 원복.
> 이번 작업에서 "상품 이미지 생성도 NanoBanana 지원" 문서 누락 오류도 함께 수정.

- [x] `application.yml` — `gemini.image-model` 값 롤백
- [x] 연관 문서 모델명 롤백 (`CLAUDE.md`, `GEMINI.md`, `.cursor/rules/project-context.mdc`, `docs/nsm-aimage-architecture.md`)
- [x] 소스 내 주석/JavaDoc/Swagger description 모델명 롤백 (`GeminiProperties`, `NanoBananaServiceImpl`, `PromptController`, `HumanPromptController`)
- [x] 문서 누락 수정 — "인물만 NanoBanana 지원" → "상품·인물 모두 지원" (`CLAUDE.md`, `GEMINI.md`, `.cursor/rules/project-context.mdc`)

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](requirements.md)
- 프로젝트 진입점: [`CLAUDE.md`](../CLAUDE.md)

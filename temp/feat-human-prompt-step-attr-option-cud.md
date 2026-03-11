# 인물 프롬프트 Step / Attr / Option CUD 구현

## 개요

인물 프롬프트(Tab 2)의 5단계 구조인 `HumanPromptStep`, `HumanPromptAttr`, `HumanPromptOption`에 대한
**생성(C) / 수정(U) / 삭제(D)** API를 구현한다.

상품 프롬프트의 TagGroup/Tag CRUD를 레퍼런스로 삼아 동일한 패턴으로 작성한다.

### 레퍼런스 비교

| 상품 프롬프트 (기존) | 인물 프롬프트 (신규) |
|---|---|
| `TagGroupController` | `HumanPromptController` (기존 확장) |
| `TagGroupService` / `TagGroupServiceImpl` | `HumanPromptStepService` / `HumanPromptStepServiceImpl` |
| `TagService` / `TagServiceImpl` | `HumanPromptAttrService` / `HumanPromptAttrServiceImpl` |
| — | `HumanPromptOptionService` / `HumanPromptOptionServiceImpl` |
| `TagGroupMapper` | `HumanPromptStepMapper` (기존 확장) |
| `TagMapper` | `HumanPromptAttrMapper` (신규) |
| — | `HumanPromptOptionMapper` (기존 확장) |

### 권한

상품 태그와 동일하게 적용한다.

| 기능 | 필요 권한 |
|---|---|
| 조회 | `USER` 이상 |
| 생성 / 수정 / 삭제 | `MANAGER` 이상 |

### 엔드포인트 설계

```
# Step
POST   /api/human-prompts/steps          → Step 생성
PUT    /api/human-prompts/steps/{id}     → Step 수정 (stepCode 수정 불가, stepNumber/name 수정 가능)
DELETE /api/human-prompts/steps/{id}     → Step 삭제

# Attr
POST   /api/human-prompts/attrs          → Attr 생성
PUT    /api/human-prompts/attrs/{id}     → Attr 수정
DELETE /api/human-prompts/attrs/{id}     → Attr 삭제

# Option
POST   /api/human-prompts/options        → Option 생성
PUT    /api/human-prompts/options/{id}   → Option 수정
DELETE /api/human-prompts/options/{id}   → Option 삭제
```

> 기존 `GET /api/human-prompts/steps` 조회 엔드포인트는 유지한다.
> 기존 `HumanPromptController`에 Step CUD를 추가한다 (Step 조회와 같은 컨트롤러).
> Attr, Option은 각각 별도 컨트롤러로 분리한다.

### 삭제 정책 — 사용 중 거부 (기존 Tag 패턴 동일 적용)

cascade 삭제 불허. 하위 데이터 또는 히스토리 참조가 있으면 삭제 거부.

| 삭제 대상 | 거부 조건 | 에러 코드 |
|---|---|---|
| Step | 하위 Attr이 1개라도 존재하면 거부 | `STEP_HAS_ATTRS` |
| Attr | 하위 Option이 1개라도 존재하면 거부 | `ATTR_HAS_OPTIONS` |
| Option | `human_prompt_history_option`에서 사용 중이면 거부 | `OPTION_IN_USE` |

> 레퍼런스: `TagGroupErrorCode.TAG_GROUP_HAS_TAGS`, `TagErrorCode.TAG_IN_USE`

---

## 작업 목록

### [x] 선행. 엔티티 수정 — `@NoArgsConstructor` / `@AllArgsConstructor` 추가

**대상:** `HumanPromptStep`, `HumanPromptAttr`

현재 두 엔티티는 `@Getter`, `@Builder`만 있어 MyBatis `selectById` 결과 매핑 시
기본 생성자가 없으면 런타임 오류가 발생한다. 컨벤션 규칙에 따라 4개 Lombok 어노테이션을 맞춘다.

```java
// 컨벤션 — Entity 기본 패턴
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStep { ... }
```

> `HumanPromptOption`은 이미 `@Getter`, `@Setter`, `@NoArgsConstructor`로 MyBatis setter 방식 사용 중 — 변경 불필요.

---

## Loop 1 — Step

### [x] 1-1. ErrorCode 추가 (Step)

```java
STEP_NOT_FOUND("HP010", "존재하지 않는 단계입니다.", 400),
STEP_HAS_ATTRS("HP011", "하위 세부항목이 존재하여 삭제할 수 없습니다.", 400),
```

### [x] 1-2. HumanPromptStepMapper 기존 확장

**파일:** `domain/prompt/mapper/HumanPromptStepMapper.java`

기존 `selectAllWithAttrsAndOptions()` 유지, 아래 메서드 추가:

- `Optional<HumanPromptStep> selectById(@Param("id") Long id)`
- `void insert(HumanPromptStep step)`
- `void update(HumanPromptStep step)`
- `void deleteById(@Param("id") Long id)`

**XML:** `mybatis/mappers/prompt/HumanPromptStepMapper.xml` 기존 파일에 SQL 추가

### [x] 1-3. HumanPromptAttrMapper에 `existsByStepId` 추가

**파일:** `domain/prompt/mapper/HumanPromptAttrMapper.java`

> 아직 이 파일이 없으므로 신규 생성. 이번 루프에서는 Step 삭제 시 하위 Attr 확인용 메서드 1개만 추가.
> 나머지 메서드는 Loop 2에서 완성한다.

- `boolean existsByStepId(@Param("stepId") Long stepId)`

**XML:** `mybatis/mappers/prompt/HumanPromptAttrMapper.xml` (신규 생성, 이번 루프 SQL만 포함)

### [x] 1-4. Step DTO 신규 생성

**request**
- `HumanPromptStepCreateReqDto` — `stepNumber(필수)`, `stepCode(필수)`, `name(필수)`
- `HumanPromptStepUpdateReqDto` — `stepNumber`, `name` (stepCode는 식별자이므로 수정 불가)

**response**
- `HumanPromptStepSimpleRespDto` — `id`, `stepNumber`, `stepCode`, `name` + `static from(HumanPromptStep)` 메서드

### [x] 1-5. HumanPromptStepService / Impl 신규 생성

**파일:** `domain/prompt/service/HumanPromptStepService.java` (인터페이스)
**파일:** `domain/prompt/service/HumanPromptStepServiceImpl.java` (구현체)

```java
HumanPromptStepSimpleRespDto createStep(HumanPromptStepCreateReqDto dto);
HumanPromptStepSimpleRespDto updateStep(Long id, HumanPromptStepUpdateReqDto dto);
void deleteStep(Long id);
```

- 수정 시 존재 여부 확인 → 없으면 `STEP_NOT_FOUND`
- 삭제 시 존재 여부 확인 → 없으면 `STEP_NOT_FOUND`
- 삭제 시 하위 Attr 존재 여부 확인 → 있으면 `STEP_HAS_ATTRS`

### [x] 1-6. HumanPromptController 기존 확장 (Step CUD)

```
POST   /api/human-prompts/steps          createStep()   MANAGER 이상
PUT    /api/human-prompts/steps/{id}     updateStep()   MANAGER 이상
DELETE /api/human-prompts/steps/{id}     deleteStep()   MANAGER 이상
```

### [x] 1-7. HumanPromptStepServiceTest 작성

> CUD 테스트는 `@Transactional` + `@Rollback` 필수.

- `[성공] Step 생성`
- `[성공] Step 수정 - stepNumber / name 변경`
- `[예외] Step 수정 - 미존재`
- `[성공] Step 삭제 - 하위 Attr 없을 때`
- `[예외] Step 삭제 - 미존재`
- `[예외] Step 삭제 - 하위 Attr 존재 시 거부`

---

## Loop 2 — Attr

### [ ] 2-1. ErrorCode 추가 (Attr)

```java
ATTR_NOT_FOUND("HP012", "존재하지 않는 세부항목입니다.", 400),
ATTR_HAS_OPTIONS("HP013", "하위 선택지가 존재하여 삭제할 수 없습니다.", 400),
```

### [ ] 2-2. HumanPromptAttrMapper 완성

**파일:** `domain/prompt/mapper/HumanPromptAttrMapper.java`

Loop 1에서 추가한 `existsByStepId` 유지, 아래 메서드 추가:

- `Optional<HumanPromptAttr> selectById(@Param("id") Long id)`
- `List<HumanPromptAttr> selectList(@Param("stepId") Long stepId)` — stepId null이면 전체
- `void insert(HumanPromptAttr attr)`
- `void update(HumanPromptAttr attr)`
- `void deleteById(@Param("id") Long id)`

**XML:** `mybatis/mappers/prompt/HumanPromptAttrMapper.xml` 기존 파일에 SQL 추가

### [ ] 2-3. HumanPromptOptionMapper에 `existsByAttrId` 추가

**파일:** `domain/prompt/mapper/HumanPromptOptionMapper.java`

> Attr 삭제 시 하위 Option 확인용. 나머지 메서드는 Loop 3에서 완성한다.

기존 `selectByIds(List<Long> ids)` 유지, 아래 메서드 추가:

- `boolean existsByAttrId(@Param("attrId") Long attrId)`

**XML:** `mybatis/mappers/prompt/HumanPromptOptionMapper.xml` 기존 파일에 SQL 추가

### [ ] 2-4. Attr DTO 신규 생성

**request**
- `HumanPromptAttrCreateReqDto` — `stepId(필수)`, `name(필수)`, `isMultiSelect(필수)`, `isRequired(필수)`, `sortOrder`
- `HumanPromptAttrUpdateReqDto` — `name`, `isMultiSelect`, `isRequired`, `sortOrder`

**response**
- `HumanPromptAttrRespDto` — `id`, `stepId`, `name`, `isMultiSelect`, `isRequired`, `sortOrder` + `static from(HumanPromptAttr)` 메서드

### [ ] 2-5. HumanPromptAttrService / Impl 신규 생성

**파일:** `domain/prompt/service/HumanPromptAttrService.java` (인터페이스)
**파일:** `domain/prompt/service/HumanPromptAttrServiceImpl.java` (구현체)

```java
HumanPromptAttrRespDto createAttr(HumanPromptAttrCreateReqDto dto);
HumanPromptAttrRespDto updateAttr(Long id, HumanPromptAttrUpdateReqDto dto);
void deleteAttr(Long id);
```

- 생성 시 `stepId` 유효성 검증 → 없으면 `STEP_NOT_FOUND` ← StepMapper.selectById 사용 (Loop 1 완료 전제)
- 수정 시 존재 여부 확인 → 없으면 `ATTR_NOT_FOUND`
- 삭제 시 존재 여부 확인 → 없으면 `ATTR_NOT_FOUND`
- 삭제 시 하위 Option 존재 여부 확인 → 있으면 `ATTR_HAS_OPTIONS`

### [ ] 2-6. HumanPromptAttrController 신규 생성

```
POST   /api/human-prompts/attrs          createAttr()   MANAGER 이상
PUT    /api/human-prompts/attrs/{id}     updateAttr()   MANAGER 이상
DELETE /api/human-prompts/attrs/{id}     deleteAttr()   MANAGER 이상
```

### [ ] 2-7. HumanPromptAttrServiceTest 작성

> CUD 테스트는 `@Transactional` + `@Rollback` 필수.

- `[성공] Attr 생성`
- `[예외] Attr 생성 - 존재하지 않는 stepId`
- `[성공] Attr 수정 - name / isMultiSelect / isRequired / sortOrder 변경`
- `[예외] Attr 수정 - 미존재`
- `[성공] Attr 삭제 - 하위 Option 없을 때`
- `[예외] Attr 삭제 - 미존재`
- `[예외] Attr 삭제 - 하위 Option 존재 시 거부`

---

## Loop 3 — Option

### [ ] 3-1. ErrorCode 추가 (Option)

```java
OPTION_NOT_FOUND("HP014", "존재하지 않는 선택지입니다.", 400),
OPTION_IN_USE("HP015", "프롬프트 이력에서 사용 중인 선택지는 삭제할 수 없습니다.", 400),
```

### [ ] 3-2. HumanPromptOptionMapper 완성

**파일:** `domain/prompt/mapper/HumanPromptOptionMapper.java`

Loop 2에서 추가한 `existsByAttrId` 유지, 아래 메서드 추가:

- `Optional<HumanPromptOption> selectById(@Param("id") Long id)`
- `List<HumanPromptOption> selectList(@Param("attrId") Long attrId)` — attrId null이면 전체
- `void insert(HumanPromptOption option)`
- `void update(HumanPromptOption option)`
- `void deleteById(@Param("id") Long id)`

**XML:** `mybatis/mappers/prompt/HumanPromptOptionMapper.xml` 기존 파일에 SQL 추가

### [ ] 3-3. HumanPromptHistoryOptionMapper에 `existsByOptionId` 추가

**파일:** `domain/prompt/mapper/HumanPromptHistoryOptionMapper.java`

> 레퍼런스: `GoodsPromptHistoryTagMapper.existsByTagId()` (boolean 반환 패턴 동일 적용)

기존 메서드 유지, 아래 메서드 추가:

- `boolean existsByOptionId(@Param("optionId") Long optionId)`

**XML:** `mybatis/mappers/prompt/HumanPromptHistoryOptionMapper.xml` 기존 파일에 SQL 추가

### [ ] 3-4. Option DTO 신규 생성

**request**
- `HumanPromptOptionCreateReqDto` — `attrId(필수)`, `name(필수)`, `promptValue(필수)`, `sortOrder`
- `HumanPromptOptionUpdateReqDto` — `name`, `promptValue`, `sortOrder`

**response**
- `HumanPromptOptionRespDto` — `id`, `attrId`, `name`, `promptValue`, `sortOrder` + `static from(HumanPromptOption)` 메서드

### [ ] 3-5. HumanPromptOptionService / Impl 신규 생성

**파일:** `domain/prompt/service/HumanPromptOptionService.java` (인터페이스)
**파일:** `domain/prompt/service/HumanPromptOptionServiceImpl.java` (구현체)

```java
HumanPromptOptionRespDto createOption(HumanPromptOptionCreateReqDto dto);
HumanPromptOptionRespDto updateOption(Long id, HumanPromptOptionUpdateReqDto dto);
void deleteOption(Long id);
```

- 생성 시 `attrId` 유효성 검증 → 없으면 `ATTR_NOT_FOUND` ← AttrMapper.selectById 사용 (Loop 2 완료 전제)
- 수정 시 존재 여부 확인 → 없으면 `OPTION_NOT_FOUND`
- 삭제 시 존재 여부 확인 → 없으면 `OPTION_NOT_FOUND`
- 삭제 시 히스토리 참조 여부 확인 → 있으면 `OPTION_IN_USE`

### [ ] 3-6. HumanPromptOptionController 신규 생성

```
POST   /api/human-prompts/options        createOption()   MANAGER 이상
PUT    /api/human-prompts/options/{id}   updateOption()   MANAGER 이상
DELETE /api/human-prompts/options/{id}   deleteOption()   MANAGER 이상
```

### [ ] 3-7. HumanPromptOptionServiceTest 작성

> CUD 테스트는 `@Transactional` + `@Rollback` 필수.

- `[성공] Option 생성`
- `[예외] Option 생성 - 존재하지 않는 attrId`
- `[성공] Option 수정 - name / promptValue / sortOrder 변경`
- `[예외] Option 수정 - 미존재`
- `[성공] Option 삭제 - 히스토리 미참조`
- `[예외] Option 삭제 - 미존재`
- `[예외] Option 삭제 - 히스토리 사용 중`

---

## 마무리

### [ ] F-1. PromptServiceTests Suite 업데이트

`PromptServiceTests.java`에 신규 테스트 클래스 3개 추가:

```java
@SelectClasses({
    PromptServiceTest.class,
    HumanPromptServiceTest.class,
    HumanPromptStepServiceTest.class,   // 신규
    HumanPromptAttrServiceTest.class,   // 신규
    HumanPromptOptionServiceTest.class  // 신규
})
```

---

## 루프별 의존 관계 요약

```
선행
 └── HumanPromptStep / HumanPromptAttr 엔티티 @NoArgsConstructor / @AllArgsConstructor 추가

Loop 1 (Step)
 └── StepMapper.selectById 완성 → Loop 2 선행 조건

Loop 2 (Attr)  ← Loop 1 완료 후 진행
 └── AttrMapper.selectById 완성 → Loop 3 선행 조건

Loop 3 (Option)  ← Loop 2 완료 후 진행

마무리
 └── Suite 업데이트
```

# feat/category-depth-3 — 상품 카테고리 4단계 → 3단계 변경 작업 계획

> **브랜치**: `feat/category-depth-3`
> **작성일**: 2026-03-18
> **전제 시나리오**: `DCS(세분류)` 제거 → `LCS(대)/MCS(중)/SCS(소)` 3단계 유지, SCS가 최종 선택 단계

---

## 변경 요약

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| 계층 | LCS → MCS → SCS → DCS (4단계) | LCS → MCS → SCS (3단계) |
| 최종 선택 타입 | DCS (세분류) | SCS (소분류) |
| CategoryType enum | LCS / MCS / SCS / DCS | LCS / MCS / SCS |
| 프롬프트 생성 시 검증 | `category_type == DCS` | `category_type == SCS` |
| 에러 메시지 | "세분류(DCS) 카테고리를 선택해야 합니다" | "소분류(SCS) 카테고리를 선택해야 합니다" |

---

## 영향 범위 파일 목록

### Java 소스 — 로직 변경

| 파일 | 변경 내용 |
|---|---|
| `CategoryType.java` | `DCS` enum 값 제거 |
| `CategoryServiceImpl.java` | `EXPECTED_PARENT_TYPE` map에서 `DCS → SCS` 항목 제거 |
| `PromptServiceImpl.java` | DCS 검증 → SCS 검증 + 인라인 주석 수정 |
| `PromptErrorCode.java` | `INVALID_CATEGORY_TYPE` 메시지 수정 |

### Java 소스 — JavaDoc / 주석만 수정

| 파일 | 수정 위치 |
|---|---|
| `Category.java` (entity) | 클래스 JavaDoc: "4단계 계층 구조: LCS > MCS > SCS > DCS" |
| `CategoryCreateReqDto.java` | JavaDoc: "LCS 생성 시 null, MCS/SCS/DCS 생성 시 상위 id" |
| `GoodsPromptCreateReqDto.java` | JavaDoc: "최종 선택한 DCS 카테고리 ID" |
| `GeminiService.java` | `@param goodsCategoryName`: "LCS→DCS 전체 경로" |
| `CategoryMapper.java` | `selectCategoryPath` JavaDoc: "LCS→MCS→SCS→DCS" |
| `PromptHistoryRespDto.java` | `categoryPath` 필드 인라인 주석: `"식품 > 건강식품 > 약재 > 인삼"` |

### 테스트 코드

| 파일 | 변경 내용 |
|---|---|
| `CategoryServiceTest.java` | 4단계 계층 테스트 → 3단계로 전환, DCS 참조 제거 |
| `PromptServiceTest.java` | `DCS_카테고리_생성()` 헬퍼 → 3단계로 수정, DCS 참조 제거 |

### MyBatis XML

| 파일 | 변경 내용 |
|---|---|
| `CategoryMapper.xml` | `selectCategoryPath` 주석 수정 (SQL 변경 불필요) |

### DB / 스키마

| 파일 | 변경 내용 |
|---|---|
| `db/schema.sql` | category 테이블 주석 수정 (4단계→3단계, DCS 언급 제거) |
| DB 실제 데이터 | 기존 DCS 카테고리 데이터 처리 (아래 "DB 마이그레이션" 참조) |

### 문서 및 룰

| 파일 | 변경 내용 |
|---|---|
| `docs/requirements.md` | 3-3절 카테고리 설명, 조회 흐름 (4→3단계) |
| `docs/erd.md` | category 테이블 설명, 계층 구조 예시, ERD mermaid |
| `docs/todo.md` | 신규 feat 완료 항목 추가 |
| `CLAUDE.md` | 카테고리 설명 (대/중/소/세 → 대/중/소) |
| `GEMINI.md` | 카테고리 설명 (4단계→3단계, DCS 참조 전체 제거) |
| `.cursor/rules/project-context.mdc` | 카테고리 계층 설명 수정 |
| `.cursor/rules/coding-convention.mdc` | `CategoryType` enum 예시에서 `DCS` 제거 |

---

## 세부 작업 계획

### STEP 1. `CategoryType.java` — enum 수정

```java
// 변경 전
public enum CategoryType {
    LCS, // 대분류
    MCS, // 중분류
    SCS, // 소분류
    DCS  // 세분류
}

// 변경 후
public enum CategoryType {
    LCS, // 대분류
    MCS, // 중분류
    SCS  // 소분류 (최종 선택 단계)
}
```

---

### STEP 2. `CategoryServiceImpl.java` — 계층 검증 맵 수정

```java
// 변경 전
private static final Map<CategoryType, CategoryType> EXPECTED_PARENT_TYPE = Map.of(
    CategoryType.MCS, CategoryType.LCS,
    CategoryType.SCS, CategoryType.MCS,
    CategoryType.DCS, CategoryType.SCS   // ← 제거
);

// 변경 후
private static final Map<CategoryType, CategoryType> EXPECTED_PARENT_TYPE = Map.of(
    CategoryType.MCS, CategoryType.LCS,
    CategoryType.SCS, CategoryType.MCS
);
```

---

### STEP 3. `PromptErrorCode.java` — 에러 메시지 수정

```java
// 변경 전
INVALID_CATEGORY_TYPE("P005", "프롬프트 생성에는 세분류(DCS) 카테고리를 선택해야 합니다.", 400),

// 변경 후
INVALID_CATEGORY_TYPE("P005", "프롬프트 생성에는 소분류(SCS) 카테고리를 선택해야 합니다.", 400),
```

---

### STEP 4. `PromptServiceImpl.java` — DCS 검증 → SCS 검증 + 인라인 주석

```java
// 변경 전 (검증 조건)
if (CategoryType.DCS != category.getCategoryType()) {
    throw new BizException(PromptErrorCode.INVALID_CATEGORY_TYPE,
        "DCS 카테고리가 아닙니다. categoryType=" + category.getCategoryType());
}
// DCS 이름만 보내면 맥락이 불분명하므로 LCS→DCS 전체 경로를 조합하여 전달

// 변경 후
if (CategoryType.SCS != category.getCategoryType()) {
    throw new BizException(PromptErrorCode.INVALID_CATEGORY_TYPE,
        "SCS 카테고리가 아닙니다. categoryType=" + category.getCategoryType());
}
// SCS 이름만 보내면 맥락이 불분명하므로 LCS→SCS 전체 경로를 조합하여 전달
```

---

### STEP 5. Java JavaDoc / 인라인 주석 수정 (6개 파일)

#### `Category.java` (entity)
```java
// 변경 전
* 4단계 계층 구조: LCS(대분류) > MCS(중분류) > SCS(소분류) > DCS(세분류)

// 변경 후
* 3단계 계층 구조: LCS(대분류) > MCS(중분류) > SCS(소분류)
```

#### `CategoryCreateReqDto.java`
```java
// 변경 전
* parentId — LCS 생성 시 null, MCS/SCS/DCS 생성 시 상위 카테고리 id

// 변경 후
* parentId — LCS 생성 시 null, MCS/SCS 생성 시 상위 카테고리 id
```

#### `GoodsPromptCreateReqDto.java`
```java
// 변경 전
* goodsCategoryId: 최종 선택한 DCS 카테고리 ID (상품군)

// 변경 후
* goodsCategoryId: 최종 선택한 SCS 카테고리 ID (상품군)
```

#### `GeminiService.java`
```java
// 변경 전
* @param goodsCategoryName 선택한 상품 카테고리명 (LCS→DCS 전체 경로)

// 변경 후
* @param goodsCategoryName 선택한 상품 카테고리명 (LCS→SCS 전체 경로)
```

#### `CategoryMapper.java`
```java
// 변경 전
* DCS 카테고리 ID로 LCS→MCS→SCS→DCS 전체 경로 이름 목록을 반환한다.
* 반환 순서: LCS 이름, MCS 이름, SCS 이름, DCS 이름

// 변경 후
* SCS 카테고리 ID로 LCS→MCS→SCS 전체 경로 이름 목록을 반환한다.
* 반환 순서: LCS 이름, MCS 이름, SCS 이름
```

#### `PromptHistoryRespDto.java`
```java
// 변경 전
private String categoryPath;     // "식품 > 건강식품 > 약재 > 인삼"

// 변경 후
private String categoryPath;     // "식품 > 건강식품 > 인삼"
```

---

### STEP 6. `CategoryMapper.xml` — `selectCategoryPath` 주석 수정

쿼리 자체는 재귀 CTE 구조라 단계 수에 무관하게 동작하므로 **SQL 수정 불필요**.
주석만 수정.

```xml
<!-- 변경 전 -->
<!-- DCS id → 상위 계층을 재귀적으로 탐색하여 LCS→MCS→SCS→DCS 이름 목록 반환 -->

<!-- 변경 후 -->
<!-- SCS id → 상위 계층을 재귀적으로 탐색하여 LCS→MCS→SCS 이름 목록 반환 -->
```

---

### STEP 7. `db/schema.sql` — 주석 수정

```sql
-- 변경 전
-- 설명: 상품 카테고리 4단계 계층 (LCS → MCS → SCS → DCS)
--       self-reference 구조로 관리. LCS는 parent_id = NULL
-- category_type VARCHAR(10) NOT NULL,  -- LCS / MCS / SCS / DCS

-- 변경 후
-- 설명: 상품 카테고리 3단계 계층 (LCS → MCS → SCS)
--       self-reference 구조로 관리. LCS는 parent_id = NULL
-- category_type VARCHAR(10) NOT NULL,  -- LCS / MCS / SCS
```

---

### STEP 8. 테스트 코드 수정

#### `CategoryServiceTest.java`

| 기존 테스트 | 변경 내용 |
|---|---|
| `성공_카테고리_생성_4단계_정상계층` | → `성공_카테고리_생성_3단계_정상계층` (DCS 생성 제거) |
| `@EnumSource(names = {"MCS", "SCS", "DCS"})` | `DCS` 제거 → `{"MCS", "SCS"}` |
| `예외_카테고리_생성_잘못된_부모_타입` `@CsvSource` | DCS 관련 케이스 6개 제거 (`MCS,DCS` / `SCS,DCS` / `DCS,LCS` / `DCS,MCS` / `DCS,SCS` / `DCS,DCS`) |
| `List.of(MCS, SCS, DCS)` / `List.of(SCS, DCS)` / `DCS == wrongParentType` 분기 | DCS 관련 분기 제거 |
| `성공_카테고리_목록_조회_4단계_드릴다운` | → `성공_카테고리_목록_조회_3단계_드릴다운` (DCS 단계 제거) |
| `예외_카테고리_삭제_사용_중` | DCS 생성 단계 제거, SCS를 최종 카테고리로 변경 |

#### `PromptServiceTest.java`

| 기존 | 변경 내용 |
|---|---|
| `DCS_카테고리_생성()` 헬퍼 (LCS→MCS→SCS→DCS 4단계) | SCS가 leaf → LCS→MCS→SCS 3단계로 수정, 메서드명 `SCS_카테고리_생성()` 으로 변경 |
| `예외_프롬프트_생성_DCS가_아닌_카테고리()` | → `예외_프롬프트_생성_SCS가_아닌_카테고리()` (메서드명 + DisplayName) |
| 4단계 인삼 예시 (`categoryPath` 검증) | 3단계 경로로 수정 |

---

### STEP 9. 문서 및 룰 업데이트

#### `docs/requirements.md`

- 3-3절: "최대 4단계 계층 (대분류 > 중분류 > 소분류 > 세분류)" → "3단계 계층 (대분류 > 중분류 > 소분류)"
- 카테고리 예시: `식품 > 건강식품 > 약재 > 인삼` → `식품 > 건강식품 > 인삼`
- 카테고리 조회 흐름: 4단계 드릴다운 → 3단계로 수정 (DCS 단계 제거)
- 5절 Category 엔티티: `category_type` 값에서 `DCS=세분류` 제거

#### `docs/erd.md`

- 테이블 목록: "4단계 계층(LCS→MCS→SCS→DCS)" → "3단계 계층(LCS→MCS→SCS)"
- category 테이블 상단 설명 수정
- `category_type` 컬럼 설명: `LCS / MCS / SCS / DCS` → `LCS / MCS / SCS`
- 계층 구조 예시: `식품 > 건강식품 > 약재 > 인삼(DCS)` → `식품 > 건강식품 > 인삼(SCS)`
- ERD mermaid 다이어그램 주석 수정

#### `CLAUDE.md`

- "4단계 계층: LCS(대) > MCS(중) > SCS(소) > DCS(세분류)" → "3단계 계층: LCS(대) > MCS(중) > SCS(소)"
- "DCS를 최종 선택하여 프롬프트 생성에 사용" → "SCS를 최종 선택..."
- `category_type` 컬럼 설명에서 `DCS` 제거

#### `GEMINI.md`

- `docs/requirements.md`와 동일한 내용 수정 (4단계→3단계, DCS 참조 전체 제거)
- 카테고리 조회 흐름 수정
- `category_type` 컬럼 설명에서 `DCS` 제거

#### `.cursor/rules/project-context.mdc`

- "4단계 계층: LCS(대분류) > MCS(중분류) > SCS(소분류) > DCS(세분류)" → 3단계로 수정

#### `.cursor/rules/coding-convention.mdc`

- "카테고리 계층 타입 상수" 섹션의 `CategoryType` enum 예시에서 `DCS  // 세분류` 제거

#### `docs/todo.md`

- 신규 `feat/category-depth-3` 완료 항목 추가

---

### STEP 10. DB 마이그레이션

> ⚠️ 기존 DB에 `DCS` 카테고리 데이터가 있다면 처리 필요

**개발 환경 (Docker 컨테이너)**: 컨테이너 볼륨 재생성으로 해결
```bash
docker-compose down -v
docker-compose up -d
```

**운영 환경 (실 데이터 있을 경우)**: 별도 마이그레이션 SQL 작성 필요
```sql
-- DCS 카테고리를 SCS로 변환 (부모-자식 관계 재정리 필요)
-- 또는 DCS 데이터 삭제 후 SCS 레벨에서 재입력
```

> 현재 PoC 단계이므로 **컨테이너 재생성으로 처리**.

---

## 작업 순서 (권장)

```
1. CategoryType.java                  ← enum 수정 (컴파일 에러로 수정 범위 자동 확인)
2. CategoryServiceImpl.java           ← EXPECTED_PARENT_TYPE 맵 수정
3. PromptErrorCode.java               ← 메시지 수정
4. PromptServiceImpl.java             ← DCS → SCS 검증 + 인라인 주석
5. JavaDoc / 인라인 주석 수정 (6개)
   - Category.java
   - CategoryCreateReqDto.java
   - GoodsPromptCreateReqDto.java
   - GeminiService.java
   - CategoryMapper.java
   - PromptHistoryRespDto.java
6. CategoryMapper.xml                 ← 주석만
7. db/schema.sql                      ← 주석만
8. 테스트 코드 수정
   - CategoryServiceTest.java
   - PromptServiceTest.java
9. 문서 및 룰 업데이트
   - docs/requirements.md
   - docs/erd.md
   - CLAUDE.md
   - GEMINI.md
   - .cursor/rules/project-context.mdc
   - .cursor/rules/coding-convention.mdc
   - docs/todo.md
10. 테스트 실행 후 확인 (AllLayersTests)
11. DB 컨테이너 재생성
```

---

## 완료 기준 (Definition of Done)

- [ ] `CategoryType` enum에 `DCS` 없음
- [ ] 카테고리 생성 시 3단계 계층 검증 정상 동작
- [ ] 프롬프트 생성 시 SCS 타입 검증 정상 동작
- [ ] 전체 테스트 (`AllLayersTests`) 통과
- [ ] 프로젝트 내 `DCS` / `세분류` / `4단계` 문자열 잔존 없음 (주요 파일 기준)
- [ ] 문서 (requirements.md / erd.md / CLAUDE.md / GEMINI.md) 동기화 완료
- [ ] DB 컨테이너 재생성 후 정상 구동

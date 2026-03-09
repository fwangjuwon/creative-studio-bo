# feat: 이력 조회 상세화

## 목표

이력 조회 응답에서 유저가 프롬프트 생성 시 입력했던 **모든 파라미터**를 볼 수 있게 한다.

| 유형 | 현재 | 목표 |
|------|------|------|
| 상품 프롬프트 | categoryId, tagIds (ID만) | 카테고리 전체 경로명, 태그명 목록, 프롬프트 스타일 |
| 인물 프롬프트 | (없음) | 선택 옵션명 목록, 생성 목적, 추가 지시사항, 프롬프트 스타일 |
| 공통 | promptType | + promptStyle (NANO_BANANA / MID_JOURNEY) |

---

## 진행 단계

### STEP 1. DB 스키마 변경 + 문서 업데이트

> 이후 모든 단계의 기반. 가장 먼저 확정해야 함.

- [ ] 1-1. `prompt_history_tag` → `goods_prompt_history_tag` 테이블명 변경
- [ ] 1-2. `prompt_history` 테이블에 컬럼 추가
  - `prompt_style` VARCHAR(20) NOT NULL DEFAULT 'NANO_BANANA'  — 'NANO_BANANA' / 'MID_JOURNEY'
  - `purpose` TEXT  — 인물 프롬프트 생성 목적 (nullable, 상품은 null)
  - `additional_notes` TEXT  — 인물 프롬프트 추가 지시사항 (nullable, 상품은 null)
- [ ] 1-3. `human_prompt_history_option` 신규 테이블 생성
  - 컬럼: id, prompt_history_id, option_id, 감사 컬럼 4개
  - 인덱스: `idx_human_prompt_history_option_history_id` ON (prompt_history_id)
- [ ] 1-4. `db/schema.sql` 반영
- [ ] 1-5. `docs/erd.md` 반영

---

### STEP 2. Entity / DTO 변경

- [ ] 2-1. `PromptHistoryTag` → `GoodsPromptHistoryTag` 클래스명 변경 (테이블명 일치)
- [ ] 2-2. `PromptHistory` entity에 필드 추가
  - `promptStyle`, `purpose`, `additionalNotes`
- [ ] 2-3. `HumanPromptHistoryOption` entity 신규 생성
  - 필드: id, promptHistoryId, optionId, 감사 컬럼 4개
- [ ] 2-4. `PromptHistoryRespDto` 확장
  - 기존 `categoryId`, `tagIds` 유지 (하위 호환)
  - 추가: `categoryPath`(String, "식품 > 건강식품 > 약재 > 인삼"), `tagNames`(List<String>)
  - 추가: `promptStyle`, `purpose`, `additionalNotes`
  - 추가: `optionNames`(List<String>) — 인물 프롬프트 선택 옵션 한국어명 목록

---

### STEP 3. Mapper 변경

- [ ] 3-1. `PromptHistoryTagMapper` → `GoodsPromptHistoryTagMapper` 이름 변경
  - Java 인터페이스 파일명 변경
  - XML 파일명 변경 (`TagMapper.xml` → `GoodsPromptHistoryTagMapper.xml`)
  - XML namespace 변경
- [ ] 3-2. `PromptHistoryMapper.xml` 쿼리 수정
  - INSERT: `prompt_style`, `purpose`, `additional_notes` 컬럼 추가
  - SELECT (`selectById`, `selectList`): 신규 컬럼 포함, 카테고리명 조회용 쿼리 추가
- [ ] 3-3. `HumanPromptHistoryOptionMapper` 신규 생성
  - Java 인터페이스: `insertBatch`, `selectOptionNamesByHistoryId`
  - XML: INSERT(배치), SELECT(option_id JOIN human_prompt_option → name)

---

### STEP 4. Service 변경

- [ ] 4-1. `PromptServiceImpl.createGoodsPrompt` 수정
  - `PromptHistory` 저장 시 `promptStyle` 포함
  - 현재: `GoodsPromptCreateReqDto`에 `promptStyle` 필드 없음 → 요청 DTO에 추가 필요
- [ ] 4-2. `HumanPromptServiceImpl.createPrompt` 수정
  - `PromptHistory` 저장 시 `promptStyle`, `purpose`, `additionalNotes` 포함
  - 선택 옵션 `human_prompt_history_option` 배치 INSERT 추가
- [ ] 4-3. 이력 조회 수정 (`getPromptHistories` 및 단건 조회)
  - 상품 프롬프트: `category` 테이블 JOIN → `categoryPath` 조합
  - 상품 프롬프트: `goods_prompt_history_tag` + `tag` JOIN → `tagNames`
  - 인물 프롬프트: `human_prompt_history_option` + `human_prompt_option` JOIN → `optionNames`
  - `PromptHistoryRespDto`에 전체 필드 채워서 반환

---

### STEP 5. 요청 DTO 변경

- [ ] 5-1. `GoodsPromptCreateReqDto`에 `promptStyle` 필드 추가 (기본값: NANO_BANANA)
  - `HumanPromptCreateReqDto`는 이미 `promptStyle` 있음 → 변경 불필요

---

### STEP 6. 테스트 코드

각 STEP 완료 후 해당 도메인 ServiceTest 작성/수정.

- [ ] 6-1. 상품 프롬프트 생성 테스트 — `promptStyle` 저장 검증
- [ ] 6-2. 인물 프롬프트 생성 테스트 — 옵션 매핑 저장, `purpose`/`additionalNotes` 저장 검증
- [ ] 6-3. 이력 조회 테스트 — 카테고리명/태그명/옵션명 포함 여부 검증

---

## 영향 파일 목록

| 파일 | 변경 유형 |
|------|---------|
| `db/schema.sql` | 테이블명 변경, 컬럼 추가, 신규 테이블 |
| `docs/erd.md` | 문서 업데이트 |
| `PromptHistory.java` (entity) | 필드 추가 |
| `PromptHistoryTag.java` → `GoodsPromptHistoryTag.java` | 이름 변경 |
| `HumanPromptHistoryOption.java` | 신규 생성 |
| `PromptHistoryRespDto.java` | 필드 확장 |
| `GoodsPromptCreateReqDto.java` | `promptStyle` 추가 |
| `PromptHistoryMapper.java` | 쿼리 수정 |
| `PromptHistoryTagMapper.java` → `GoodsPromptHistoryTagMapper.java` | 이름 변경 |
| `HumanPromptHistoryOptionMapper.java` | 신규 생성 |
| `PromptHistoryMapper.xml` | 컬럼 추가, JOIN 쿼리 추가 |
| `PromptHistoryTagMapper.xml` → `GoodsPromptHistoryTagMapper.xml` | 이름 변경 |
| `HumanPromptHistoryOptionMapper.xml` | 신규 생성 |
| `PromptServiceImpl.java` | 저장 + 조회 수정 |
| `HumanPromptServiceImpl.java` | 저장 + 조회 수정 |
| 관련 ServiceTest | 테스트 추가 |

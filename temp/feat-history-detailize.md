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

### ✅ STEP 1. DB 스키마 변경 + 문서 업데이트

- [x] 1-1. `prompt_history_tag` → `goods_prompt_history_tag` 테이블명 변경
- [x] 1-2. `prompt_history` 테이블에 컬럼 추가
  - `prompt_style` VARCHAR(20) NOT NULL DEFAULT 'NANO_BANANA'
  - `purpose` TEXT (nullable)
  - `additional_notes` TEXT (nullable)
- [x] 1-3. `human_prompt_history_option` 신규 테이블 + 인덱스 생성
- [x] 1-4. `db/schema.sql` 반영
- [x] 1-5. `docs/erd.md` 반영 (테이블 목록, 상세 정의, mermaid ERD)

> ⚠️ Docker 이미지 재빌드 필요: `docker-compose down -v && docker-compose up --build -d`

---

### ✅ STEP 2. Entity / DTO 변경

- [x] 2-1. `PromptHistoryTag` → `GoodsPromptHistoryTag` 클래스명 변경 (파일 신규 생성 + 구 파일 삭제)
- [x] 2-2. `PromptHistory` entity에 필드 추가 (`promptStyle`, `purpose`, `additionalNotes`)
- [x] 2-3. `HumanPromptHistoryOption` entity 신규 생성
- [x] 2-4. `PromptHistoryRespDto` 확장
  - `categoryPath`, `tagNames`, `promptStyle`, `purpose`, `additionalNotes`, `optionNames` 필드 추가
  - `from()` → `fromGoods(history, categoryPath, tagIds, tagNames)` / `fromHuman(history, optionNames)` 분리

---

### ✅ STEP 3. Mapper 변경

- [x] 3-1. `PromptHistoryTagMapper` → `GoodsPromptHistoryTagMapper` 이름 변경 (Java + XML, 구 파일 삭제)
  - XML 테이블명 `prompt_history_tag` → `goods_prompt_history_tag`
  - `selectTagNamesByPromptHistoryId` 신규 추가 (tag JOIN)
- [x] 3-2. `PromptHistoryMapper.xml` 쿼리 수정
  - INSERT / SELECT: `prompt_style`, `purpose`, `additional_notes` 컬럼 추가
- [x] 3-3. `HumanPromptHistoryOptionMapper` 신규 생성 (Java + XML)
  - `insertBatch`, `selectOptionNamesByHistoryId` (human_prompt_option JOIN)

---

### ✅ STEP 4. Service 변경

- [x] 4-1. `PromptServiceImpl.createGoodsPrompt` 수정
  - `promptStyle` 저장, 응답에 `categoryPath` + `tagNames` 포함
- [x] 4-2. `HumanPromptServiceImpl.createPrompt` 수정
  - `promptStyle`, `purpose`, `additionalNotes` 저장
  - `human_prompt_history_option` 배치 INSERT 추가
  - 응답에 `optionNames` 포함
- [x] 4-3. `getPromptHistories` 조회 수정
  - PRODUCT: `categoryMapper.selectCategoryPath` + `selectTagNamesByPromptHistoryId` JOIN
  - PERSON: `selectOptionNamesByHistoryId` JOIN

---

### ✅ STEP 5. 요청 DTO 변경

- [x] 5-1. `GoodsPromptCreateReqDto`에 `promptStyle` 필드 추가 (미입력 시 NANO_BANANA 기본값)

---

### 🔲 STEP 6. 테스트 코드

> Docker 재빌드 완료 후 진행

- [ ] 6-1. 상품 프롬프트 생성 테스트 — `promptStyle` 저장, `categoryPath`/`tagNames` 응답 검증
- [ ] 6-2. 인물 프롬프트 생성 테스트 — 옵션 매핑 저장, `purpose`/`additionalNotes`/`optionNames` 응답 검증
- [ ] 6-3. 이력 조회 테스트 — 상품/인물 각각 전체 필드 포함 여부 검증

---

## 영향 파일 목록

| 파일 | 변경 유형 | 상태 |
|------|---------|------|
| `db/schema.sql` | 테이블명 변경, 컬럼 추가, 신규 테이블 | ✅ |
| `docs/erd.md` | 문서 업데이트 | ✅ |
| `PromptHistory.java` (entity) | 필드 추가 | ✅ |
| `PromptHistoryTag.java` → `GoodsPromptHistoryTag.java` | 이름 변경 | ✅ |
| `HumanPromptHistoryOption.java` | 신규 생성 | ✅ |
| `PromptHistoryRespDto.java` | 필드 확장, factory 메서드 분리 | ✅ |
| `GoodsPromptCreateReqDto.java` | `promptStyle` 추가 | ✅ |
| `PromptHistoryTagMapper.java` → `GoodsPromptHistoryTagMapper.java` | 이름 변경 | ✅ |
| `HumanPromptHistoryOptionMapper.java` | 신규 생성 | ✅ |
| `PromptHistoryMapper.xml` | 컬럼 추가 | ✅ |
| `PromptHistoryTagMapper.xml` → `GoodsPromptHistoryTagMapper.xml` | 이름 변경, 태그명 JOIN 추가 | ✅ |
| `HumanPromptHistoryOptionMapper.xml` | 신규 생성 | ✅ |
| `PromptServiceImpl.java` | 저장 + 조회 수정 | ✅ |
| `HumanPromptServiceImpl.java` | 저장 + 조회 수정 | ✅ |
| `TagServiceImpl.java` | Mapper 참조 업데이트 | ✅ |
| `TagServiceTest.java` | Mapper/Entity 참조 업데이트 | ✅ |
| 관련 ServiceTest (신규) | 테스트 추가 | 🔲 |

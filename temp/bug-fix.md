# 코드 검사 결과 — 버그 & 요구사항 불일치

> 검사 일자: 2026-03-09
> 검사 대상: 전체 도메인 (user / category / tag / prompt)

---

## [버그 1 - Critical] HumanPromptServiceImpl — 인물 프롬프트 일일 한도 체크 누락

- **파일**: `domain/prompt/service/HumanPromptServiceImpl.java`
- **상태**: ✅ 완료

### 현상
요구사항 3-1에 "USER는 하루 N회 프롬프트 생성 요청 가능, 제한 초과 시 에러 응답"이 명시되어 있다.
`PromptServiceImpl.createGoodsPrompt()`에는 `countTodayByUserId`로 일일 한도 체크 로직이 구현되어 있으나,
`HumanPromptServiceImpl.createPrompt()`에는 해당 체크가 **전혀 없다**.

결과적으로 인물(PERSON) 이미지 프롬프트는 `daily_limit`에 관계없이 **무제한 생성** 가능하다.

### 추가 비대칭 버그
`countTodayByUserId` 쿼리는 `prompt_type` 구분 없이 PRODUCT + PERSON 합산 카운트한다.
→ 인물 프롬프트를 한도 없이 대량 생성한 후 상품 프롬프트를 요청하면 즉시 한도 초과 오류 발생.

### 수정 방향
`HumanPromptServiceImpl.createPrompt()` 첫 부분에 User 조회 + 일일 한도 체크 로직 추가.
`PromptServiceImpl`의 패턴과 동일하게 구현.

```java
// 추가 필요 — PromptServiceImpl 패턴과 동일
User user = userMapper.selectById(userId)
        .orElseThrow(() -> new BizException(HumanPromptErrorCode.USER_NOT_FOUND, ...));

int todayCount = promptHistoryMapper.countTodayByUserId(userId);
if (todayCount >= user.getDailyLimit()) {
    throw new BizException(HumanPromptErrorCode.DAILY_LIMIT_EXCEEDED);
}
```

---

## [버그 2 - Minor] PromptHistoryMapper.xml — updateImageUrl에서 final_rvis_pic 누락

- **파일**: `resources/mybatis/mappers/prompt/PromptHistoryMapper.xml`
- **상태**: ✅ 완료

### 현상
`updateImageUrl` UPDATE 쿼리에서 `final_rvis_dttm`(최종수정일시)만 업데이트하고
`final_rvis_pic`(최종수정자 login_id)은 업데이트하지 않는다.

```xml
<!-- 현재 — final_rvis_pic 누락 -->
<update id="updateImageUrl">
    UPDATE prompt_history
    SET image_url       = #{imageUrl},
        final_rvis_dttm = NOW()
    WHERE id = #{id}
</update>
```

코딩 컨벤션: "공통 감사 컬럼 — `final_rvis_pic` / `final_rvis_dttm`은 항상 쌍으로 업데이트"

### 수정 방향
1. `PromptHistoryMapper` 인터페이스의 `updateImageUrl()` 파라미터에 `finalRvisPic` 추가
2. XML 쿼리에 `final_rvis_pic = #{finalRvisPic}` 추가
3. 호출부(`HumanPromptServiceImpl.generateImage()`)에서 `SecurityUtil.getCurrentLoginId()` 값 전달

---

## [요구사항 불일치] requirements.md 컬럼명 오기재

- **파일**: `docs/requirements.md` 섹션 5, `domain/prompt/entity/PromptHistory.java` JavaDoc
- **상태**: ⬜ 미처리

### 현상
| 위치 | 기재 내용 |
|------|----------|
| `requirements.md` 섹션 5 | `goods_category_id BIGINT` |
| `schema.sql` (실제 DB) | `category_id BIGINT` |
| `PromptHistory.java` 필드명 | `categoryId` |
| `PromptHistory.java` JavaDoc 주석 | `goods_category_id: 논리 FK → Category DCS` |

DB와 코드는 `category_id` / `categoryId`로 일치하므로 **기능적 문제는 없다**.
단, 요구사항 문서와 Entity JavaDoc이 `goods_category_id`로 잘못 기재되어 있어 혼동 여지가 있다.

### 수정 방향
- `docs/requirements.md` 섹션 5: `goods_category_id` → `category_id` 수정
- `PromptHistory.java` JavaDoc 주석: `goods_category_id` → `category_id` 수정

---

## [설계 미흡 1] TagServiceImpl.deleteTag() — 사용 중인 태그 삭제 방어 없음

- **파일**: `domain/tag/service/TagServiceImpl.java`
- **상태**: ✅ 완료

### 현상
코딩 컨벤션: "FK 제약조건은 DB에 없고, 참조 무결성은 Service 레이어에서 보장"

| 위치 | 참조 무결성 체크 |
|------|----------------|
| `TagGroupServiceImpl.deleteTagGroup()` | 소속 Tag 존재 시 삭제 방지 ✅ |
| `TagServiceImpl.deleteTag()` | `prompt_history_tag` 참조 체크 ❌ |

사용 중인 태그를 삭제하면 `prompt_history_tag.tag_id`가 고아 레코드가 된다.
(FK CONSTRAINT 없으므로 DB 오류는 없지만 논리적 무결성 파괴)

### 수정 방향
1. `PromptHistoryTagMapper`에 `existsByTagId(@Param("tagId") Long tagId)` 메서드 추가
2. `PromptHistoryTagMapper.xml`에 해당 쿼리 추가
3. `TagServiceImpl.deleteTag()`에서 호출하여 참조 중이면 `BizException` throw

---

## [설계 미흡 2] CategoryServiceImpl.deleteCategory() — PromptHistory 참조 체크 없음

- **파일**: `domain/category/service/CategoryServiceImpl.java`
- **상태**: ✅ 완료

### 현상
`deleteCategory()`는 하위 카테고리 존재 체크는 수행하지만,
`prompt_history.category_id`에서 해당 카테고리를 참조 중인지는 체크하지 않는다.

DCS 카테고리를 삭제하면 기존 프롬프트 히스토리에서 `category_id`로 카테고리 정보를 조회할 수 없게 된다.
([설계 미흡 1]과 동일한 논리 FK 정합성 보장 원칙 위반)

### 수정 방향
1. `PromptHistoryMapper`에 `existsByCategoryId(@Param("categoryId") Long categoryId)` 메서드 추가
2. `PromptHistoryMapper.xml`에 해당 쿼리 추가
3. `CategoryServiceImpl.deleteCategory()`에서 호출하여 참조 중이면 `BizException` throw

---

## 처리 현황

| # | 심각도 | 내용 | 상태 |
|---|--------|------|------|
| 버그 1 | 🔴 Critical | 인물 프롬프트 일일 한도 체크 누락 | ✅ 완료 |
| 버그 2 | 🟡 Minor | updateImageUrl final_rvis_pic 누락 | ✅ 완료 |
| 불일치 | 🟢 Minor | requirements.md 컬럼명 오기재 | ⬜ 미처리 |
| 설계 1 | 🟡 Minor | 사용 중인 태그 삭제 방어 없음 | ✅ 완료 |
| 설계 2 | 🟡 Minor | 참조 중인 카테고리 삭제 방어 없음 | ✅ 완료 |

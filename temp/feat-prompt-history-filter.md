# feat/prompt-history-filter 작업계획서

## 개요

프롬프트 이력 조회(`GET /api/prompts`) 기능에 **조건 기반 필터링**을 추가한다.
현재는 전체 이력을 페이징으로만 제공하고 있으며, 이번 작업으로 아래 조건으로 필터링할 수 있게 된다.

| 필터 파라미터 | 설명 | 대응 DB 컬럼 |
|---|---|---|
| `loginId` | 특정 사용자의 이력만 조회 | `init_regi_pic` (login_id 저장) |
| `promptType` | 상품/인물 구분 | `prompt_type` (`PRODUCT` \| `PERSON`) |
| `promptStyle` | 프롬프트 종류 | `prompt_style` (`NANO_BANANA` \| `MID_JOURNEY`) |
| `dateFrom` | 생성일 시작 (포함) | `init_regi_dttm` |
| `dateTo` | 생성일 종료 (포함) | `init_regi_dttm` |

> `loginId` 필터는 `user` 테이블 JOIN 없이 `init_regi_pic` 컬럼으로 직접 비교한다.
> (`init_regi_pic`에 생성자의 `login_id` 값이 저장되므로 JOIN 불필요)

---

## 브랜치

```
feat/prompt-history-filter
```

---

## 변경 파일 목록

| 구분 | 파일 | 변경 내용 |
|---|---|---|
| 신규 | `domain/prompt/dto/request/PromptHistorySearchReqDto.java` | 검색 조건 + 페이징 통합 DTO |
| 수정 | `domain/prompt/mapper/PromptHistoryMapper.java` | `selectList` 시그니처 변경 |
| 수정 | `resources/mybatis/mappers/prompt/PromptHistoryMapper.xml` | 동적 SQL 추가 |
| 수정 | `domain/prompt/service/PromptService.java` | 인터페이스 시그니처 변경 |
| 수정 | `domain/prompt/service/PromptServiceImpl.java` | 구현체 로직 수정 |
| 수정 | `domain/prompt/controller/PromptController.java` | 파라미터 타입 + Swagger 업데이트 |
| 수정 | `test/.../prompt/service/PromptServiceTest.java` | 필터 케이스 추가 |

---

## 세부 작업 계획

### Step 1 — `PromptHistorySearchReqDto` 생성

`PageReqDto`를 extend하여 페이징 + 검색 파라미터를 하나의 DTO로 통합한다.

```java
public class PromptHistorySearchReqDto extends PageReqDto {
    private String loginId;     // nullable
    private String promptType;  // nullable, "PRODUCT" | "PERSON"
    private String promptStyle; // nullable, "NANO_BANANA" | "MID_JOURNEY"
    private LocalDate dateFrom; // nullable, 생성일 시작 (포함)
    private LocalDate dateTo;   // nullable, 생성일 종료 (포함)
}
```

- `PageReqDto`를 extend하므로 `PageHelperUtil.pagedQuery(dto, ...)` 호출 그대로 사용 가능
- 단일 객체 파라미터이므로 `@Param` 불필요 (컨벤션 준수)
- Swagger `@Schema` 어노테이션으로 각 파라미터 설명 추가

### Step 2 — `PromptHistoryMapper` 시그니처 변경

```java
// 변경 전
List<PromptHistory> selectList();

// 변경 후
List<PromptHistory> selectList(PromptHistorySearchReqDto dto);
```

### Step 3 — `PromptHistoryMapper.xml` 동적 SQL

`<where>` + `<if>` 조합으로 각 조건이 null이면 무시.
날짜 범위는 인라인 CDATA로 `<`, `>=` 처리 (컨벤션 준수).

```xml
<select id="selectList" resultType="PromptHistory">
    SELECT ...
    FROM prompt_history
    <where>
        <if test="loginId != null and loginId != ''">
            AND init_regi_pic = #{loginId}
        </if>
        <if test="promptType != null and promptType != ''">
            AND prompt_type = #{promptType}
        </if>
        <if test="promptStyle != null and promptStyle != ''">
            AND prompt_style = #{promptStyle}
        </if>
        <if test="dateFrom != null">
            AND init_regi_dttm <![CDATA[ >= ]]> #{dateFrom}::date
        </if>
        <if test="dateTo != null">
            AND init_regi_dttm <![CDATA[ < ]]> #{dateTo}::date + INTERVAL '1 day'
        </if>
    </where>
    ORDER BY init_regi_dttm DESC
</select>
```

### Step 4 — `PromptService` 인터페이스 시그니처 변경

```java
// 변경 전
PageRespDto<PromptHistoryRespDto> getPromptHistories(PageReqDto pageReqDto);

// 변경 후
PageRespDto<PromptHistoryRespDto> getPromptHistories(PromptHistorySearchReqDto searchReqDto);
```

### Step 5 — `PromptServiceImpl` 수정

```java
// 변경 전
PageInfo<PromptHistory> pageInfo = PageHelperUtil.pagedQuery(pageReqDto, promptHistoryMapper::selectList);

// 변경 후
PageInfo<PromptHistory> pageInfo = PageHelperUtil.pagedQuery(searchReqDto, () -> promptHistoryMapper.selectList(searchReqDto));
```

### Step 6 — `PromptController` 수정

```java
// 변경 전
public CommonResponse<PageRespDto<PromptHistoryRespDto>> getPromptHistories(
        @ModelAttribute @Valid PageReqDto pageReqDto)

// 변경 후
public CommonResponse<PageRespDto<PromptHistoryRespDto>> getPromptHistories(
        @ModelAttribute @Valid PromptHistorySearchReqDto searchReqDto)
```

- `@Operation` description에 필터 파라미터 설명 추가

---

## 테스트 케이스

`PromptServiceTest.java`에 아래 케이스 추가/수정한다.

| 테스트 메서드명 | DisplayName | 비고 |
|---|---|---|
| `성공_프롬프트_이력_조회_전체` | `[성공] 프롬프트 이력 조회 - 전체 (필터 없음)` | 기존 테스트 SearchReqDto 기반으로 수정 |
| `성공_프롬프트_이력_조회_loginId_필터` | `[성공] 프롬프트 이력 조회 - loginId 필터` | 특정 사용자 이력만 반환 검증 |
| `성공_프롬프트_이력_조회_상품_타입_필터` | `[성공] 프롬프트 이력 조회 - promptType PRODUCT` | 상품 프롬프트만 반환 검증 |
| `성공_프롬프트_이력_조회_인물_타입_필터` | `[성공] 프롬프트 이력 조회 - promptType PERSON` | 인물 프롬프트만 반환 검증 |
| `성공_프롬프트_이력_조회_스타일_필터` | `[성공] 프롬프트 이력 조회 - promptStyle 필터` | NANO_BANANA / MID_JOURNEY |
| `성공_프롬프트_이력_조회_날짜_범위_필터` | `[성공] 프롬프트 이력 조회 - 날짜 범위 필터` | dateFrom~dateTo 범위 검증 |
| `성공_프롬프트_이력_조회_복합_필터` | `[성공] 프롬프트 이력 조회 - 복합 필터` | 여러 조건 조합 검증 |
| `없음_프롬프트_이력_조회_조건_불일치` | `[없음] 프롬프트 이력 조회 - 조건 불일치` | 결과 0건 → 빈 페이지 반환 검증 |

---

## 진행 상태

- [ ] 브랜치 생성 (`feat/prompt-history-filter`)
- [ ] Step 1: `PromptHistorySearchReqDto` 생성
- [ ] Step 2: `PromptHistoryMapper` 시그니처 변경
- [ ] Step 3: `PromptHistoryMapper.xml` 동적 SQL 추가
- [ ] Step 4: `PromptService` 인터페이스 시그니처 변경
- [ ] Step 5: `PromptServiceImpl` 수정
- [ ] Step 6: `PromptController` 수정
- [ ] 테스트 코드 작성
- [ ] 테스트 실행 확인
- [ ] `docs/todo.md` 업데이트

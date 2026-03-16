# 페이징 적용 작업 계획 — 프롬프트 히스토리 조회

> 작성일: 2026-03-16
> 브랜치명 예시: `feat/paging-prompt-history`

---

## 배경

`GET /api/prompts` (히스토리 조회) 가 현재 전체 데이터를 한 번에 반환한다.
데이터 누적에 따른 응답 지연 및 메모리 부담을 방지하기 위해 PageHelper 기반 페이징을 도입한다.

---

## Oracle → PostgreSQL 차이

참조 코드(`temp/paging-ref`)는 Oracle 기반 프로젝트에서 작성되었으나,
PageHelper는 JDBC URL을 통해 DB 방언을 자동 감지하므로 **SQL 변경 없음**.

| 항목 | Oracle (참조) | PostgreSQL (현재 프로젝트) |
|---|---|---|
| 페이징 SQL | `ROWNUM` / `FETCH FIRST` | `LIMIT` / `OFFSET` |
| PageHelper 처리 | 자동 | 자동 (방언 자동 감지) |
| 추가 설정 | 불필요 | 불필요 |

---

## 변경 범위

### 신규 생성 (3개)

| 파일 | 위치 |
|---|---|
| `PageReqDto.java` | `common/page/` |
| `PageRespDto.java` | `common/page/` |
| `PageHelperUtil.java` | `common/page/` |

### 수정 (4개)

| 파일 | 변경 내용 |
|---|---|
| `build.gradle` | pagehelper 의존성 추가 |
| `PromptService.java` | `getPromptHistories()` 시그니처 변경 |
| `PromptServiceImpl.java` | 구현 변경 |
| `PromptController.java` | 파라미터 추가, 반환 타입 변경 |

### 테스트 수정 (1개)

| 파일 | 변경 내용 |
|---|---|
| `PromptServiceTest.java` | 페이징 케이스 추가, 기존 히스토리 조회 테스트 수정 |

---

## 단계별 작업 내용

### Step 1. 의존성 추가 — `build.gradle`

```groovy
implementation 'com.github.pagehelper:pagehelper-spring-boot-starter:2.1.0'
```

> 추가 위치: `// MyBatis` 블록 아래

---

### Step 2. 공통 페이징 모듈 생성

패키지: `com.nsmall.ns_creative_studio_bo.common.page`

#### 2-1. `PageReqDto.java`

참조 코드에서 `@Setter` 제거 (프로젝트 컨벤션: Request DTO에 `@Setter` 사용 안 함).
`setPageNum()` / `setPageSize()` 호출 부분은 생성자(`new PageReqDto(1, 10)`)로 대체.

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이징 요청")
public class PageReqDto {

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer pageNum = 1;

    @Schema(description = "페이지 크기 (1~1000)", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 1000, message = "페이지 크기는 1000 이하여야 합니다")
    private Integer pageSize = 10;
}
```

#### 2-2. `PageRespDto.java`

참조 코드와 동일 (`record` 타입, `of(PageInfo)` 팩토리 메서드).

#### 2-3. `PageHelperUtil.java`

참조 코드와 동일 (`pagedQuery`, `pagedResponse` 두 정적 메서드).

---

### Step 3. `PromptService.java` — 인터페이스 변경

```java
// Before
List<PromptHistoryRespDto> getPromptHistories();

// After
PageRespDto<PromptHistoryRespDto> getPromptHistories(PageReqDto pageReqDto);
```

JavaDoc 수정:
- 파라미터 `@param pageReqDto 페이징 요청 (페이지 번호, 크기)` 추가
- 반환 타입 설명 수정

---

### Step 4. `PromptServiceImpl.java` — 구현 변경

```java
// Before
@Override
public List<PromptHistoryRespDto> getPromptHistories() {
    List<PromptHistory> histories = promptHistoryMapper.selectList();
    return histories.stream()
            .map(history -> { ... })
            .toList();
}

// After
@Override
public PageRespDto<PromptHistoryRespDto> getPromptHistories(PageReqDto pageReqDto) {
    return PageHelperUtil.pagedResponse(pageReqDto,
            () -> promptHistoryMapper.selectList().stream()
                    .map(history -> { ... })
                    .toList()
    );
}
```

> ⚠️ **주의**: `Supplier` 안에서 `stream().map().toList()`를 호출하면
> PageHelper가 `selectList()` SQL에 LIMIT/OFFSET을 먼저 적용한 후에 변환이 실행된다.
> 즉 N+1 반복 횟수도 페이지 크기만큼으로 제한되는 효과가 있다.

---

### Step 5. `PromptController.java` — 엔드포인트 변경

```java
// Before
@GetMapping
public CommonResponse<List<PromptHistoryRespDto>> getPromptHistories() {
    return CommonResponse.success(promptService.getPromptHistories());
}

// After
@GetMapping
public CommonResponse<PageRespDto<PromptHistoryRespDto>> getPromptHistories(
        @ModelAttribute @Valid PageReqDto pageReqDto) {
    return CommonResponse.success(promptService.getPromptHistories(pageReqDto));
}
```

Swagger `@Operation` description 수정:
> "Role 구분 없이 전체 히스토리를 페이지 단위로 조회한다. pageNum(기본 1), pageSize(기본 10)."

---

### Step 6. `PromptServiceTest.java` — 테스트 수정

#### 기존 테스트 수정

`getPromptHistories()` → `getPromptHistories(new PageReqDto())` 로 호출부 변경.
검증 방식도 `List` → `PageRespDto` 기준으로 수정 (`.items()` 접근).

```java
// Before
List<PromptHistoryRespDto> result = promptService.getPromptHistories();
assertThat(result).isNotEmpty();

// After
PageRespDto<PromptHistoryRespDto> result = promptService.getPromptHistories(new PageReqDto());
assertThat(result.items()).isNotEmpty();
```

#### 신규 추가 테스트

```
[성공] 히스토리 조회 페이징 - 첫 페이지 메타정보 검증
  → pageNum=1, pageSize=10, hasPreviousPage=false 검증

[성공] 히스토리 조회 페이징 - pageSize 제한 적용 확인
  → pageSize=1 로 요청 시 items().size() == 1 검증

[성공] 히스토리 조회 페이징 - 빈 결과
  → 데이터 없을 때 total=0, items=[] 검증
```

> CUD 없는 조회 테스트이므로 `@Transactional` / `@Rollback` 불필요.

---

## 최종 API 스펙

```
GET /api/prompts?pageNum=1&pageSize=10

응답:
{
  "code": 200,
  "message": "OK",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 42,
    "pages": 5,
    "items": [ ... ],
    "hasPreviousPage": false,
    "hasNextPage": true
  }
}
```

- `pageNum`, `pageSize` 쿼리 파라미터 미입력 시 기본값 적용 (1, 10)
- 정렬 기준: 기존 `ORDER BY init_regi_dttm DESC` 유지 (XML 변경 없음)

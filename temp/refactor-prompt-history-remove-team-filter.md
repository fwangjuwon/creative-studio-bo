# refactor: 프롬프트 이력 조회 팀 필터 제거

## 변경 배경

현재 `GET /api/prompts` (프롬프트 히스토리 조회)는 Role 기반 범위 제한이 적용되어 있다.

| Role | 현재 조회 범위 |
|---|---|
| `USER` | 같은 `team_name`을 가진 유저들의 히스토리만 |
| `MANAGER` / `ADMIN` | 전체 |

이를 **Role과 무관하게 모두 전체 조회**로 변경한다.

---

## 변경 파일 목록 (6개)

| # | 파일 | 변경 유형 |
|---|---|---|
| 1 | `PromptHistoryMapper.xml` | SQL 단순화 |
| 2 | `PromptHistoryMapper.java` | 메서드 시그니처 변경 + JavaDoc |
| 3 | `PromptService.java` | JavaDoc 수정 |
| 4 | `PromptServiceImpl.java` | 비즈니스 로직 단순화 |
| 5 | `PromptController.java` | Swagger description 수정 |
| 6 | `PromptServiceTest.java` | 테스트 케이스 수정 |

> `docs/requirements.md`, `CLAUDE.md`, `GEMINI.md`의 조회 권한 설명도 함께 수정한다.

---

## 파일별 변경 상세

### 1. `PromptHistoryMapper.xml`

`selectList` SQL에서 `<if test="teamName != null">` 조건절(JOIN + WHERE) 전체 제거.

```xml
<!-- AS-IS -->
<select id="selectList" resultType="PromptHistory">
    SELECT ph.id, ...
    FROM prompt_history ph
    <if test="teamName != null">
        JOIN "user" u ON ph.user_id = u.id
        WHERE u.team_name = #{teamName}
    </if>
    ORDER BY ph.init_regi_dttm DESC
</select>

<!-- TO-BE -->
<select id="selectList" resultType="PromptHistory">
    SELECT id, ...
    FROM prompt_history
    ORDER BY init_regi_dttm DESC
</select>
```

- JOIN 제거 → `ph.` 테이블 별칭도 불필요하므로 제거
- `teamName` 파라미터 바인딩(`#{teamName}`) 제거

---

### 2. `PromptHistoryMapper.java`

`selectList` 메서드에서 `@Param("teamName") String teamName` 파라미터 제거.

```java
// AS-IS
List<PromptHistory> selectList(@Param("teamName") String teamName);

// TO-BE
List<PromptHistory> selectList();
```

JavaDoc의 팀 필터 설명(`teamName != null → ...` 블록) 제거.

---

### 3. `PromptService.java` (인터페이스)

클래스 레벨 JavaDoc의 히스토리 조회 권한 범위 설명 수정.

```java
// AS-IS
 * 히스토리 조회는 요청 사용자의 권한에 따라 범위가 달라진다:
 *   - USER  → 같은 팀(team_name) 유저들의 히스토리만 조회
 *   - MANAGER / ADMIN → 전체 히스토리 조회

// TO-BE
 * 히스토리 조회는 Role 구분 없이 전체 이력을 반환한다.
```

`getPromptHistories` 메서드 시그니처에서 `requestUserId` 파라미터 제거.

```java
// AS-IS
List<PromptHistoryRespDto> getPromptHistories(Long requestUserId);

// TO-BE
List<PromptHistoryRespDto> getPromptHistories();
```

> **이유**: userId는 팀 필터를 위해 전달하던 값이었으므로, 필터 제거와 함께 파라미터도 불필요해진다.

---

### 4. `PromptServiceImpl.java`

`getPromptHistories` 메서드 전체를 단순화한다.

```java
// AS-IS
@Override
public List<PromptHistoryRespDto> getPromptHistories(Long requestUserId) {
    User user = userMapper.selectById(requestUserId)
            .orElseThrow(() -> new BizException(PromptErrorCode.USER_NOT_FOUND, ...));

    // USER 권한: 같은 팀의 히스토리만, MANAGER/ADMIN: 전체
    // USER인데 팀 미배정(teamName == null)이면 빈 목록 반환
    String teamName = null;
    if ("USER".equals(user.getRole())) {
        if (user.getTeamName() == null) {
            log.warn("...");
            return List.of();
        }
        teamName = user.getTeamName();
    }

    List<PromptHistory> histories = promptHistoryMapper.selectList(teamName);
    // ... stream().map() ...
}

// TO-BE
@Override
public List<PromptHistoryRespDto> getPromptHistories() {
    List<PromptHistory> histories = promptHistoryMapper.selectList();
    // ... stream().map() (기존 매핑 로직 동일) ...
}
```

제거 대상:
- `userMapper.selectById(requestUserId)` 호출 (User 조회)
- Role 분기 로직 (`"USER".equals(user.getRole())`)
- teamName 분기 로직 (null 체크 + 빈 목록 반환)
- 관련 log.warn

유지 대상:
- `histories.stream().map()` 매핑 로직 (`PRODUCT` / `HUMAN` 분기, categoryPath, tagIds, tagNames 조립)

---

### 5. `PromptController.java`

`getPromptHistories` 핸들러의 Swagger `@Operation` description 수정.

```java
// AS-IS
@Operation(
    summary = "프롬프트 히스토리 조회",
    description = "요청 사용자의 권한에 따라 히스토리 범위가 다르다. " +
                  "USER → 같은 팀 히스토리만, MANAGER/ADMIN → 전체.",
    ...
)
// AS-IS 메서드 파라미터
public CommonResponse<List<PromptHistoryRespDto>> getPromptHistories(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    return CommonResponse.success(promptService.getPromptHistories(userDetails.getId()));
}

// TO-BE
@Operation(
    summary = "프롬프트 히스토리 조회",
    description = "Role 구분 없이 전체 히스토리를 조회한다.",
    ...
)
// TO-BE 메서드 파라미터 — @AuthenticationPrincipal 제거
public CommonResponse<List<PromptHistoryRespDto>> getPromptHistories() {
    return CommonResponse.success(promptService.getPromptHistories());
}
```

---

### 6. `PromptServiceTest.java`

#### 삭제할 테스트 케이스 (3개)

| 메서드명 | 삭제 이유 |
|---|---|
| `성공_히스토리_조회_USER_같은팀만` | 팀 필터 로직 자체가 제거됨 |
| `성공_히스토리_조회_USER_팀_미배정_빈목록` | "팀 미배정 → 빈 목록" 로직 제거됨 |
| `예외_히스토리_조회_사용자_미존재` | userId 파라미터 및 User 조회 로직 자체가 제거됨 |

#### 수정할 테스트 케이스 (2개 → 1개로 통합)

`성공_히스토리_조회_MANAGER_전체`, `성공_히스토리_조회_ADMIN_전체` 두 케이스를
아래 추가 테스트 1개로 **통합 대체**한다.

#### 추가할 테스트 케이스 (1개)

```
[성공] 히스토리 조회 - ADMIN/MANAGER/USER 모두 동일한 전체 결과 반환
```

**목적**: Role 기반 분기가 완전히 제거되었음을 명시적으로 증명한다.

**시나리오**:
1. ADMIN, MANAGER, USER(다른 팀) 3명의 유저 생성
2. 서로 다른 팀에 속한 유저 3명의 히스토리를 각각 삽입
3. `getPromptHistories()` 를 세 번 호출 (각각 ADMIN, MANAGER, USER 관점은 없지만, 동일 메서드로 세 결과 획득)
4. 세 결과의 히스토리 id 집합이 모두 동일한지 검증

**검증 코드 (예시)**:
```java
List<Long> result1 = promptService.getPromptHistories().stream()
        .map(PromptHistoryRespDto::getId).toList();
List<Long> result2 = promptService.getPromptHistories().stream()
        .map(PromptHistoryRespDto::getId).toList();

assertThat(result1).containsExactlyInAnyOrderElementsOf(result2);
assertThat(result1).contains(adminHistoryId, managerHistoryId, userHistoryId);
```

> `getPromptHistories()`는 파라미터가 없으므로 호출자 Role과 무관하게 항상 동일한 결과를 반환한다.
> 세 번 호출해서 결과가 같다는 것은 "필터가 존재하지 않음"을 증명하는 것과 동치이다.

---

## 문서 수정

| 파일 | 변경 내용 |
|---|---|
| `docs/requirements.md` (193~198) | 조회 권한 테이블: USER 범위를 "전체"로 수정 |
| `CLAUDE.md` | 프롬프트 조회 권한 설명 수정 |
| `GEMINI.md` | 프롬프트 조회 권한 설명 수정 |

---

## 변경 범위 요약

```
제거: Role 분기, teamName 필터, User 조회, 팀 미배정 빈목록 처리
단순화: selectList() 파라미터 없음, getPromptHistories() 파라미터 없음
테스트: 권한별 범위 테스트 5개 삭제 → ADMIN/MANAGER/USER 동일 결과 검증 테스트 1개 추가
```

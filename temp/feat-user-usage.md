# 기능: 사용자 일일 사용량 조회

## 이슈 배경

- 사용자 본인이 오늘 몇 번 사용했는지, 몇 번 남았는지 알 수 없음
- 관리자도 각 유저의 사용 현황을 쿼리 없이 확인할 방법이 없음
- 현재 일일 한도 체크용 `countTodayByUserId` 쿼리가 이미 존재하지만 API로 노출되어 있지 않음

---

## 프로젝트 컨텍스트

- Java 17, Spring Boot 3.5.11, MyBatis, PostgreSQL
- 패키지 베이스: `com.nsmall.ns_creative_studio_bo`
- MyBatis `mapUnderscoreToCamelCase=true`, type-alias 설정 있음 (클래스 짧은 이름 사용)
- 공통 응답: `CommonResponse<T>`, 서비스는 인터페이스+구현체 패턴
- Controller 반환 타입: `CommonResponse<T>` 직접 반환 (`ResponseEntity` 사용 금지)
- 성공 HTTP 상태: 무조건 200

---

## 현재 상태 분석

### 기존 쿼리 — 인덱스를 사용하지 못하는 구조

**파일**: `src/main/resources/mybatis/mappers/prompt/PromptHistoryMapper.xml`

```xml
<select id="countTodayByUserId" resultType="int">
    SELECT COUNT(*)
    FROM prompt_history
    WHERE user_id = #{userId}
      AND DATE(init_regi_dttm) = CURRENT_DATE   ← 문제: 함수 적용으로 인덱스 무효화
</select>
```

`DATE(init_regi_dttm)` 처럼 컬럼에 함수를 씌우면 B-tree 인덱스를 타지 못하고 full scan.
이 쿼리는 **사용량 조회 API**뿐 아니라 **프롬프트 생성 시 일일 한도 체크**에서도 매번 호출되므로
history가 쌓일수록 점점 느려지는 구조.

### 기존 Mapper 인터페이스

**파일**: `domain/prompt/mapper/PromptHistoryMapper.java`

```java
int countTodayByUserId(@Param("userId") Long userId);
```

### 사용처

**파일**: `domain/prompt/service/PromptServiceImpl.java` — `createGoodsPrompt()`, 인물 프롬프트 서비스도 동일

```java
int todayCount = promptHistoryMapper.countTodayByUserId(currentUserId);
if (todayCount >= user.getDailyLimit()) {
    throw new BizException(PromptErrorCode.DAILY_LIMIT_EXCEEDED);
}
```

---

## 설계 방향

**단순 쿼리 + 인덱스** 방식 채택 (PoC 단계 적합).

카운터 테이블(`user_daily_usage`) 방식은 `prompt_history`와 정합성 관리 부담이 있고
현재 트래픽 규모에서는 오버스펙이므로 제외.

### 핵심 변경 사항 3가지

1. **쿼리 수정** — `DATE()` 함수 제거, 범위 조건으로 변경하여 인덱스 활용
2. **DB 인덱스 추가** — `prompt_history(user_id, init_regi_dttm)` 복합 인덱스
3. **API 추가** — `GET /api/users/me/usage` (본인), `GET /api/users/{id}/usage` (ADMIN)

### 크로스 도메인 의존성 결정

`UserServiceImpl`에서 `PromptHistoryMapper`를 직접 주입하는 방식 채택.
이미 `PromptServiceImpl → UserMapper` (prompt → user) 크로스 도메인 의존이 존재하는 현재 관행을 수용.
구조적 정리는 별도 문서(`refactor-cross-domain-dependency.md`) 참조.

---

## 구현 계획

### Step 1 — 기존 쿼리 수정 (인덱스 친화적으로)

**`PromptHistoryMapper.xml`** 수정:

```xml
<!-- 변경 전 -->
AND DATE(init_regi_dttm) = CURRENT_DATE

<!-- 변경 후 — 범위 조건으로 인덱스 활용 -->
AND init_regi_dttm <![CDATA[ >= ]]> CURRENT_DATE
AND init_regi_dttm <![CDATA[ < ]]> CURRENT_DATE + INTERVAL '1 day'
```

> 이 수정은 사용량 조회 API와 기존 일일 한도 체크 모두에 적용됨.

---

### Step 2 — DB 인덱스 추가

마이그레이션 SQL (운영 반영 시 DBA 또는 직접 적용):

```sql
CREATE INDEX idx_prompt_history_user_date
    ON prompt_history(user_id, init_regi_dttm);
```

`user_id` 조건으로 필터 후 `init_regi_dttm` 범위 스캔 → 복합 인덱스가 두 조건 모두 커버.

---

### Step 3 — 응답 DTO 추가

**새 파일**: `domain/user/dto/response/UserUsageRespDto.java`

```java
/**
 * 사용자 일일 사용량 응답 DTO
 *
 * @author jjhoo8936
 * @since 작성일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUsageRespDto {
    private Long   userId;
    private String loginId;
    private String name;
    private int    dailyLimit;      // 일일 할당량
    private int    todayUsedCount;  // 오늘 사용 횟수
    private int    remainingCount;  // 남은 횟수 (dailyLimit - todayUsedCount, 음수 방지)

    public static UserUsageRespDto of(User user, int todayUsedCount) {
        return UserUsageRespDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .dailyLimit(user.getDailyLimit())
                .todayUsedCount(todayUsedCount)
                .remainingCount(Math.max(0, user.getDailyLimit() - todayUsedCount))
                .build();
    }
}
```

> `from()` 대신 `of()`를 사용한 이유: 단일 엔티티 변환이 아닌 User + count 두 값의 조합이기 때문.

---

### Step 4 — UserService 인터페이스에 메서드 추가

**`domain/user/service/UserService.java`** 에 추가:

```java
/** 사용자 사용량 조회 — 본인 및 ADMIN의 특정 유저 조회 모두 이 메서드 사용 */
UserUsageRespDto getUserUsage(Long userId);
```

> `getMyUsage` / `getUserUsage` 두 메서드를 분리하지 않고 `getUserUsage(Long userId)` 하나로 통일.
> 컨트롤러에서 본인 조회 시 `userDetails.getId()` 를 넘기고, ADMIN 조회 시 PathVariable `id`를 넘기면 됨.

---

### Step 5 — UserServiceImpl 구현

**`domain/user/service/UserServiceImpl.java`** 에 추가:

의존성 주입 추가 (`PromptHistoryMapper` 크로스 도메인 주입 — 현재 관행 수용):
```java
private final PromptHistoryMapper promptHistoryMapper;
```

메서드 구현:
```java
@Override
public UserUsageRespDto getUserUsage(Long userId) {
    User user = userMapper.selectById(userId)
            .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
    int todayUsedCount = promptHistoryMapper.countTodayByUserId(userId);
    return UserUsageRespDto.of(user, todayUsedCount);
}
```

---

### Step 6 — UserServiceTest에 신규 케이스 추가

**`domain/user/service/UserServiceTest.java`** 에 추가:

의존성 주입 추가:
```java
@Autowired
private PromptHistoryMapper promptHistoryMapper;
```

헬퍼 메서드 추가 (`PromptServiceTest`의 `히스토리_직접_삽입` 패턴 동일하게):
```java
private void 히스토리_직접_삽입(Long userId) {
    PromptHistory history = PromptHistory.builder()
            .userId(userId)
            .promptType("PRODUCT")
            .promptStyle("NANO_BANANA")
            .promptText("test prompt")
            .initRegiPic("test")
            .initRegiDttm(LocalDateTime.now())
            .finalRvisPic("test")
            .finalRvisDttm(LocalDateTime.now())
            .build();
    promptHistoryMapper.insert(history);
}
```

테스트 케이스:

| 케이스 | 어노테이션 | 핵심 검증 |
|--------|-----------|-----------|
| `[성공] 사용량 조회 - 오늘 사용 없음` | `@Test @Transactional @Rollback` | `todayUsedCount=0`, `remainingCount=dailyLimit` |
| `[성공] 사용량 조회 - N회 사용 후` | `@Test @Transactional @Rollback` | `todayUsedCount=N`, `remainingCount=dailyLimit-N` |
| `[성공] 사용량 조회 - 한도 초과 시 remainingCount=0` | `@Test @Transactional @Rollback` | `remainingCount >= 0` 음수 방지 검증 |
| `[예외] 사용량 조회 - 미존재 유저` | `@Test` | `USER_NOT_FOUND` 예외 |

```java
@Test
@Transactional
@Rollback
@DisplayName("[성공] 사용량 조회 - 오늘 사용 없음")
void 성공_사용량_조회_오늘_사용_없음() {
    UserRespDto user = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "usage_zero"));

    UserUsageRespDto result = userService.getUserUsage(user.getId());

    assertThat(result.getTodayUsedCount()).isEqualTo(0);
    assertThat(result.getRemainingCount()).isEqualTo(result.getDailyLimit());
}

@Test
@Transactional
@Rollback
@DisplayName("[성공] 사용량 조회 - N회 사용 후")
void 성공_사용량_조회_N회_사용_후() {
    UserRespDto user = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "usage_n"));
    히스토리_직접_삽입(user.getId());
    히스토리_직접_삽입(user.getId());

    UserUsageRespDto result = userService.getUserUsage(user.getId());

    assertThat(result.getTodayUsedCount()).isEqualTo(2);
    assertThat(result.getRemainingCount()).isEqualTo(result.getDailyLimit() - 2);
}

@Test
@Transactional
@Rollback
@DisplayName("[성공] 사용량 조회 - 한도 초과 시 remainingCount = 0")
void 성공_사용량_조회_한도_초과_시_remaining_0() {
    // dailyLimit = 2, 히스토리 3개 삽입
    UserRespDto user = userService.createUser(UserCreateReqDto.builder()
            .loginId(TEST_ID_PREFIX + "usage_over")
            .password("testPassword123!")
            .name("한도초과유저")
            .role("USER")
            .teamName("팀A")
            .dailyLimit(2)
            .build());
    히스토리_직접_삽입(user.getId());
    히스토리_직접_삽입(user.getId());
    히스토리_직접_삽입(user.getId());

    UserUsageRespDto result = userService.getUserUsage(user.getId());

    assertThat(result.getRemainingCount()).isEqualTo(0);
    assertThat(result.getTodayUsedCount()).isEqualTo(3);
}

@Test
@DisplayName("[예외] 사용량 조회 - 미존재 유저")
void 예외_사용량_조회_미존재_유저() {
    assertThatThrownBy(() -> userService.getUserUsage(Long.MAX_VALUE))
            .isInstanceOf(BizException.class)
            .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
}
```

> ⚠️ Step 1 쿼리 수정 후 **기존 `PromptServiceTest`의 `예외_프롬프트_생성_일일한도_초과` 테스트**도 반드시 실행하여 회귀 없음 확인.

---

### Step 7 — UserController에 API 추가

**`domain/user/controller/UserController.java`** 에 추가:

```java
// 본인 사용량 조회 — 모든 인증 사용자 허용 (@PreAuthorize 없음, JWT 인증만으로 접근 가능)
@Operation(summary = "내 사용량 조회", description = "오늘 사용 횟수 및 남은 횟수 확인. 모든 인증 사용자 허용.",
        security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponse(responseCode = "200", description = "조회 성공")
@GetMapping("/me/usage")
public CommonResponse<UserUsageRespDto> getMyUsage(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    return CommonResponse.success(userService.getUserUsage(userDetails.getId()));
}

// 특정 회원 사용량 조회 — ADMIN 전용
@Operation(summary = "특정 회원 사용량 조회", description = "ADMIN 전용",
        security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponse(responseCode = "200", description = "조회 성공")
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/{id}/usage")
public CommonResponse<UserUsageRespDto> getUserUsage(@PathVariable Long id) {
    return CommonResponse.success(userService.getUserUsage(id));
}
```

> `/me/usage`에 `@PreAuthorize` 없는 것은 의도된 설계 — USER / MANAGER / ADMIN 모든 Role이 본인 사용량을 조회할 수 있어야 함.
> `@AuthenticationPrincipal CustomUserDetails`는 `PromptController`에서 이미 사용 중인 패턴.

---

## API 스펙 요약

| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| GET | `/api/users/me/usage` | 모든 인증 사용자 | 본인 오늘 사용량 조회 |
| GET | `/api/users/{id}/usage` | ADMIN | 특정 유저 사용량 조회 |

**응답 예시:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userId": 5,
    "loginId": "user01",
    "name": "홍길동",
    "dailyLimit": 10,
    "todayUsedCount": 3,
    "remainingCount": 7
  }
}
```

---

## 관련 파일 목록

### 수정 대상

| 파일 | 변경 내용 |
|------|-----------|
| `mybatis/mappers/prompt/PromptHistoryMapper.xml` | `countTodayByUserId` 쿼리 — 범위 조건으로 수정 |
| `domain/user/service/UserService.java` | `getUserUsage` 메서드 추가 |
| `domain/user/service/UserServiceImpl.java` | 구현 + `PromptHistoryMapper` 의존성 주입 |
| `domain/user/controller/UserController.java` | `/me/usage`, `/{id}/usage` 엔드포인트 추가 |
| `domain/user/service/UserServiceTest.java` | `getUserUsage` 테스트 케이스 4개 추가 |

### 신규 생성 대상

| 파일 | 내용 |
|------|------|
| `domain/user/dto/response/UserUsageRespDto.java` | 사용량 응답 DTO (정적 팩토리 `of()` 포함) |

### DB (직접 반영)

```sql
CREATE INDEX idx_prompt_history_user_date
    ON prompt_history(user_id, init_regi_dttm);
```

---

## 작업 체크리스트

- [ ] Step 1: `PromptHistoryMapper.xml` — `countTodayByUserId` 범위 조건으로 수정
- [ ] Step 1 검증: `PromptServiceTest` 실행 — 기존 일일 한도 체크 회귀 확인
- [ ] Step 2: DB 인덱스 생성 SQL 반영
- [ ] Step 3: `UserUsageRespDto.java` — 신규 생성 (JavaDoc, 정적 팩토리 `of()` 필수)
- [ ] Step 4: `UserService.java` — `getUserUsage` 시그니처 추가
- [ ] Step 5: `UserServiceImpl.java` — 구현 + `PromptHistoryMapper` 의존성 주입
- [ ] Step 6: `UserServiceTest.java` — `getUserUsage` 테스트 케이스 4개 추가 및 통과 확인
- [ ] Step 7: `UserController.java` — `/me/usage`, `/{id}/usage` 엔드포인트 추가

---

## 주의사항

- `remainingCount` 계산은 `UserUsageRespDto.of()` 안에서 처리 (`Math.max(0, ...)` — 음수 방지)
- `/me/usage`에 `@PreAuthorize` 없는 것은 **의도된 설계** (모든 Role 허용), 실수가 아님
- `@AuthenticationPrincipal CustomUserDetails` 패턴은 `PromptController`에서 사용 중인 패턴과 동일하게 맞출 것
- 쿼리 수정 후 기존 일일 한도 체크 동작 회귀 테스트 필수 (`HumanPromptServiceTest`, `PromptServiceTest`)
- 새 DTO, 메서드 모두 JavaDoc 필수 (`@author jjhoo8936`, `@since` 작성일)
- `UserServiceImpl`에 `PromptHistoryMapper` 크로스 도메인 주입은 현재 관행 수용 결정 (구조 개선은 `refactor-cross-domain-dependency.md` 참조)

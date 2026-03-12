# feat: 관리자용 전체 회원 사용량 목록 조회 API

## 배경 / 목적

관리자 화면(사용자 관리 목록)에 **일일 한도(`daily_limit`)** 컬럼이 노출되어 있으나,
금일 실제 사용량이 없어 "한도 대비 얼마나 썼는지" 파악 불가.

현재 `GET /api/users/{id}/usage` 는 1명씩 조회 → 관리자가 전체 현황 파악 시 N번 API 호출 필요.

**목표**: 단일 API 호출로 전체 회원의 금일 사용량 목록을 반환한다.

---

## 프로젝트 컨텍스트

- Java 17, Spring Boot 3.5.11, MyBatis, PostgreSQL
- 패키지 베이스: `com.nsmall.ns_creative_studio_bo`
- 공통 응답: `CommonResponse<T>` — `ResponseEntity` 사용 금지, 성공 시 무조건 200
- Controller 반환 타입: `CommonResponse<T>`
- Service 패턴: 인터페이스 + 구현체 (`UserService` / `UserServiceImpl`)
- MyBatis: `resultMap` 사용 금지, `mapUnderscoreToCamelCase=true`, Type Alias 사용 (짧은 클래스명)
- `@Param` 규칙: primitive/wrapper 단일 파라미터 필수, Entity/DTO 단일 객체 불필요

---

## 관련 기존 코드

### 재사용 대상 — `UserUsageRespDto` (기존 파일, 변경 없음)

`domain/user/dto/response/UserUsageRespDto.java`

```java
// 기존 필드 구성 — 그대로 재사용 가능
private Long   userId;
private String loginId;
private String name;
private int    dailyLimit;
private int    todayUsedCount;
private int    remainingCount;

// 기존 factory method (단건용) — 목록 API에서는 사용 안 함 (SQL에서 직접 계산)
public static UserUsageRespDto of(User user, int todayUsedCount) { ... }
```

### 기존 API — 단건 조회 (변경 없음)

```
GET /api/users/me/usage       — 본인 사용량 (모든 인증 사용자)
GET /api/users/{id}/usage     — 특정 회원 사용량 (ADMIN 전용)
```

### 기존 쿼리 — `countTodayByUserId` (재사용/참조)

`PromptHistoryMapper.xml` 에 있는 범위 조건 패턴 동일하게 적용:

```sql
-- 기존 단건 카운트 쿼리와 동일한 날짜 범위 조건 사용
AND init_regi_dttm >= CURRENT_DATE
AND init_regi_dttm <  CURRENT_DATE + INTERVAL '1 day'
```

---

## 설계

### API 스펙

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| URL | `/api/users/usage` |
| 권한 | `ADMIN` 전용 (`@PreAuthorize("hasRole('ADMIN')")`) |
| 응답 | `CommonResponse<List<UserUsageRespDto>>` |
| 쿼리 횟수 | DB 1회 (JOIN) |

### 핵심 SQL — JOIN 단일 쿼리

`UserMapper.xml`에 추가:

```sql
SELECT
    u.id                                                AS user_id,
    u.login_id,
    u.name,
    u.daily_limit,
    COUNT(ph.id)                                        AS today_used_count,
    GREATEST(0, u.daily_limit - COUNT(ph.id))           AS remaining_count
FROM "user" u
LEFT JOIN prompt_history ph
    ON  ph.user_id        = u.id
    AND ph.init_regi_dttm >= CURRENT_DATE
    AND ph.init_regi_dttm <![CDATA[ < ]]> CURRENT_DATE + INTERVAL '1 day'
GROUP BY u.id, u.login_id, u.name, u.daily_limit
ORDER BY u.id ASC
```

- `u.id AS user_id` → `mapUnderscoreToCamelCase` → `userId` 자동 매핑
- `GREATEST(0, ...)` → PostgreSQL 내장 함수, `remainingCount` 음수 방지
- `LEFT JOIN` → 오늘 사용 이력 없는 유저도 `today_used_count = 0`으로 포함

### 메서드 위치 결정 — `UserMapper`

결과가 사용자 중심 데이터이므로 `UserMapper` / `UserMapper.xml`에 추가.
SQL이 `prompt_history` 테이블을 JOIN하더라도, **주 엔티티가 User**이므로 User Mapper가 적합.

> ⚠️ 기존 `UserServiceImpl`이 이미 `PromptHistoryMapper`를 의존하는 구조 (feat/user-usage에서 도입).
> 이번 작업 이후에는 `UserServiceImpl` → `PromptHistoryMapper` 의존을 제거하고
> `UserMapper.selectListWithTodayUsage()` 처럼 User 도메인 내에서 처리 가능해질 수 있다.
> (단, 기존 `getUserUsage` 단건 메서드의 리팩터링은 별도 판단)

---

## 작업 범위

### 신규/수정 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `domain/user/mapper/UserMapper.java` | `selectListWithTodayUsage()` 메서드 추가 |
| `mybatis/mappers/user/UserMapper.xml` | 위 메서드에 대한 SQL 추가 |
| `domain/user/service/UserService.java` | `getUserUsageList()` 시그니처 추가 |
| `domain/user/service/UserServiceImpl.java` | `getUserUsageList()` 구현 추가 |
| `domain/user/controller/UserController.java` | `GET /api/users/usage` 엔드포인트 추가 |
| `test/.../user/service/UserServiceTest.java` | 테스트 케이스 추가 |

---

## 구현 단계별 계획

### Step 1 — `UserMapper.java` 메서드 추가

```java
// UserMapper.java
List<UserUsageRespDto> selectListWithTodayUsage();
```

- 파라미터 없음 → `@Param` 불필요
- 반환 타입: `List<UserUsageRespDto>` — SQL 결과가 DTO 필드와 1:1 매핑됨
- 메서드 네이밍: 명확한 복합 이름 → 컨벤션 예외 허용 (`selectList`만으로는 의미 불명확)

---

### Step 2 — `UserMapper.xml` SQL 추가

```xml
<!--
    전체 회원의 금일 사용량 목록 조회 (ADMIN 전용 화면용)
    LEFT JOIN 사용: 오늘 이력 없는 유저도 today_used_count = 0 으로 포함
    GREATEST(0, ...) : remainingCount 음수 방지
-->
<select id="selectListWithTodayUsage" resultType="UserUsageRespDto">
    SELECT
        u.id                                            AS user_id,
        u.login_id,
        u.name,
        u.daily_limit,
        COUNT(ph.id)                                    AS today_used_count,
        GREATEST(0, u.daily_limit - COUNT(ph.id))       AS remaining_count
    FROM "user" u
    LEFT JOIN prompt_history ph
        ON  ph.user_id        = u.id
        AND ph.init_regi_dttm <![CDATA[ >= ]]> CURRENT_DATE
        AND ph.init_regi_dttm <![CDATA[ < ]]>  CURRENT_DATE + INTERVAL '1 day'
    GROUP BY u.id, u.login_id, u.name, u.daily_limit
    ORDER BY u.id ASC
</select>
```

---

### Step 3 — `UserService.java` 인터페이스에 시그니처 추가

```java
/**
 * 전체 회원의 금일 사용량 목록 조회 — ADMIN 전용
 */
List<UserUsageRespDto> getUserUsageList();
```

---

### Step 4 — `UserServiceImpl.java` 구현 추가

```java
@Override
public List<UserUsageRespDto> getUserUsageList() {
    return userMapper.selectListWithTodayUsage();
}
```

- 단순 위임(delegation) — 별도 비즈니스 로직 없음
- DB 쿼리 1회

---

### Step 5 — `UserController.java` 엔드포인트 추가

```java
@Operation(summary = "전체 회원 사용량 목록 조회", description = "ADMIN 전용 — 금일 사용 횟수 및 남은 횟수 포함",
        security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponse(responseCode = "200", description = "조회 성공")
@ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/usage")
public CommonResponse<List<UserUsageRespDto>> getUserUsageList() {
    return CommonResponse.success(userService.getUserUsageList());
}
```

> ⚠️ URL 충돌 주의: 기존 `GET /api/users/{id}/usage` 와 `GET /api/users/usage` 가 공존.
> Spring MVC는 정적 경로(`/usage`)를 경로 변수(`/{id}/usage`)보다 우선 매칭하므로 충돌 없음.

---

### Step 6 — `UserServiceTest.java` 테스트 케이스 추가

기존 테스트 구조(`UserServiceTest.java`)에 "사용량 목록 조회" 섹션 추가.

**추가할 테스트 케이스:**

```
[성공] 사용량 목록 조회 - 전체 회원 포함 여부 확인
  → 3명 생성 후 selectListWithTodayUsage 결과에 3명 포함 확인

[성공] 사용량 목록 조회 - 오늘 이력 없는 유저 todayUsedCount = 0
  → 히스토리 삽입 없이 생성한 유저의 todayUsedCount = 0 확인

[성공] 사용량 목록 조회 - 오늘 N회 사용한 유저 카운트 정확성
  → N건 직접 삽입 후 해당 유저의 todayUsedCount = N 확인

[성공] 사용량 목록 조회 - remainingCount 음수 방지 (한도 초과 시 0)
  → dailyLimit보다 많이 삽입한 유저의 remainingCount = 0 확인
```

---

## 작업 체크리스트

- [ ] **Step 1**: `UserMapper.java` — `selectListWithTodayUsage()` 추가
- [ ] **Step 2**: `UserMapper.xml` — SQL 추가 (날짜 범위 조건 + LEFT JOIN + GROUP BY)
- [ ] **Step 3**: `UserService.java` — `getUserUsageList()` 시그니처 추가 (JavaDoc 필수)
- [ ] **Step 4**: `UserServiceImpl.java` — `getUserUsageList()` 구현 추가
- [ ] **Step 5**: `UserController.java` — `GET /api/users/usage` 엔드포인트 추가 (Swagger 어노테이션)
- [ ] **Step 6**: `UserServiceTest.java` — 테스트 케이스 4개 추가
- [ ] **Step 6 검증**: 테스트 전체 통과 확인
- [ ] Swagger UI에서 신규 API 확인 (`/swagger-ui/index.html`)

---

## 주의사항 / 설계 결정

### 검색 조건(UserSearchReqDto) 적용 여부

관리자 화면에 이름/팀명/권한 검색 필터가 있으나, 이번 API는 **전체 목록 단순 조회**로 시작.
필요 시 `UserSearchReqDto`를 파라미터로 추가하는 방향으로 확장 가능.

```java
// 현재: 전체 조회
List<UserUsageRespDto> getUserUsageList();

// 향후 확장 시:
List<UserUsageRespDto> getUserUsageList(UserSearchReqDto dto);
```

### 페이지네이션

현재 PoC 단계 — 페이지네이션 없음. 추후 PageHelper 적용 예정 (기존 설계 방침 동일).

### 응답 DTO 재사용

`UserUsageRespDto` 그대로 재사용. 별도 List용 DTO 생성 불필요.
SQL 컬럼 alias(`user_id`, `login_id`, `today_used_count`, `remaining_count`)가 `mapUnderscoreToCamelCase`로 자동 매핑됨.

### URL 경로 충돌 없음

Spring MVC 경로 우선순위: 정적 경로 > 경로 변수
- `GET /api/users/usage` → 신규 (정적 경로, 우선 매칭)
- `GET /api/users/{id}/usage` → 기존 (`{id}` 경로 변수)

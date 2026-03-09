# 요구사항 명세서

> **프로젝트**: ns-creative-studio-bo
> **최초 작성일**: 2026-02-25
> **작성자**: jjhoo8936
> **상태**: ✅ 확정 (v1.0)

---

## 1. 프로젝트 개요

이미지 생성형 AI(nanobanana, Midjourney 등)에 전달할 **이미지 생성 프롬프트를 자동 생성**해주는 서비스의 백엔드 API 서버.

단순 채팅으로도 이미지 생성이 가능하지만, 더 좋은 결과물을 위해선 상세한 프롬프트가 필요하다.
사용자가 **상품 카테고리(어떤 상품)** 와 **이미지 스타일 태그(어떻게 보일지)** 를 선택하면,
Gemini API를 통해 최적화된 영문 프롬프트를 생성하는 것이 핵심 기능이다.

생성된 프롬프트는 팀 내에서 공유되어 성능 좋은 프롬프트를 재사용할 수 있다.

---

## 2. 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle |
| Group | `com.nsmall` |
| DB | PostgreSQL |
| ORM | MyBatis |
| 인증 | Spring Security + JWT (Stateless) |
| AI | Gemini API (`gemini-3-pro-preview` 또는 `gemini-3-flash-preview`) |
| 아키텍처 | REST API 서버 (프론트엔드 프로젝트 별도 존재) |

> **Gemini 모델 선택 기준**
> - `gemini-3-flash-preview`: 빠르고 저렴 ($0.50/$3 per 1M tokens), 프롬프트 생성처럼 반복 호출이 많은 작업에 적합
> - `gemini-3-pro-preview`: 고품질 추론 ($2/$12 per 1M tokens), 정밀한 프롬프트가 필요할 때
> - 초기에는 flash 사용 후 품질에 따라 pro로 전환 고려

---

## 3. 기능 요구사항

### 3-1. 회원 / 권한

#### 권한(Role) 정의

| Role | 설명 |
|------|------|
| `ADMIN` | 총 관리자. 모든 기능에 풀 접근 |
| `MANAGER` | USER 권한 + 카테고리·태그 CUD 권한 |
| `USER` | 하루 N회 제한 내에서 프롬프트 생성 가능, 같은 팀의 프롬프트 조회 가능 |

#### Role 구조 설계 결정

**현재 방식**: `User` 테이블의 `role VARCHAR` 단일 컬럼으로 관리

**ROLE 테이블 + USER_ROLE_RLS 테이블 분리 방식을 검토했으나 현재는 채택하지 않음.**

채택하지 않은 이유:
- 역할이 `ADMIN > MANAGER > USER` 계층 구조로 고정 → 한 유저가 복수 역할을 가지는 시나리오 없음
- 역할 동적 추가 계획 없음
- 소규모 PoC 단계에서 JOIN 복잡도만 증가
- Spring Security 연동 시 단순한 구조가 유리

아래 상황이 발생하면 RBAC 구조로 리팩토링 고려:
- 역할 종류가 늘어나 코드 수정이 잦아질 때
- 기능 단위 세밀한 권한 제어가 필요해질 때 (ex: 팀별로 다른 MANAGER 권한 범위)
- 역할 계층이 아닌 독립적 권한 조합이 필요해질 때

#### 인증 방식
- **자체 로그인만** (소셜 로그인 없음)
- ID/PW 입력 → JWT 발급 (Access Token)
- Stateless 방식
- **Access Token 유효시간**: 1시간
- **Refresh Token**: PoC 단계에서는 사용하지 않음 (만료 시 재로그인). 추후 추가 가능.

#### 회원가입
- **Closed 방식**: ADMIN이 시스템(API)을 통해 직접 계정 생성
- 일반 사용자 자가 회원가입 없음

#### 팀(Team) 관리
- 별도 Team 테이블 없이 `User` 테이블에 `team_name` 컬럼(VARCHAR)으로 관리
- 소규모 서비스에 적합한 단순 구조
- ⚠️ 팀명 변경 시 해당 팀 전체 유저의 `team_name` 업데이트 필요 (trade-off 인지함)

#### 사용량 제한 (USER)
- USER는 **하루 N회** 프롬프트 생성 요청 가능
- N은 ADMIN이 시스템에서 조정 가능 (전체 설정 또는 유저별 설정)
- 제한 초과 시 에러 응답

**사용량 체크 방식**
- `User.today_count` 컬럼 없음 (매일 초기화를 위한 배치/스케줄러 불필요)
- 프롬프트 생성 요청 시 `PromptHistory`에서 오늘 날짜 기준으로 실시간 COUNT 집계
- `COUNT 결과 >= User.daily_limit` 이면 요청 거부

```
프롬프트 생성 요청
  → PromptHistory WHERE user_id = ? AND DATE(init_regi_dttm) = 오늘 → COUNT
  → COUNT >= daily_limit → 에러 응답
  → COUNT < daily_limit  → Gemini 호출 허용
```

---

### 3-2. 프롬프트 생성 (Gemini 연동)

#### 흐름
```
사용자
  → 카테고리 선택 (상품군: 대/중/소/세분류)
  → 태그 선택 (이미지 스타일: 단일/복수 선택)
  → 프롬프트 생성 요청
  → Gemini API 호출
  → 영문 이미지 생성 프롬프트 반환
  → 사용자가 프롬프트를 복사하여 이미지 생성 AI(nanobanana, Midjourney 등)에 직접 사용
```

#### 입력값
- 카테고리 선택값 (계층형 상품군 최종 선택값)
- 태그 선택값 (여러 태그 그룹에서 선택한 값들)

#### 출력값
- **영어** 이미지 생성 프롬프트 텍스트

#### Gemini API Key 관리
- `.env` 파일의 환경변수 `GEMINI_API_KEY` 로 관리
- `application.yml`에서 `${GEMINI_API_KEY}` 로 주입

#### 이미지 생성 AI 연동
- **현재**: 이 서버는 프롬프트 텍스트만 생성, 실제 이미지 생성은 사용자가 직접 수행
- **추후**: 이미지 생성 AI 직접 연동 가능성 있음 (현재 범위 외)

---

### 3-3. 카테고리 / 태그

> **카테고리와 태그는 완전히 다른 개념**

#### 카테고리 (상품군)
- **목적**: 이미지로 생성할 **상품이 무엇인지** 지정
- **구조**: 최대 4단계 계층 (대분류 > 중분류 > 소분류 > 세분류)
- **예시**:
  ```
  식품 > 건강식품 > 약재 > 인삼
  식품 > 음료 > 주스 > 오렌지주스
  ```
- **관리**: ADMIN, MANAGER만 CUD 가능
- **조회**: 전체 회원 가능

#### 카테고리 조회 API 흐름 (단계별 드릴다운)
사용자가 프롬프트 생성 시 카테고리를 선택하는 흐름:
```
1. 대분류(LCS) 전체 목록 조회
2. 선택한 LCS의 중분류(MCS) 목록 조회 (parent_id = LCS.id)
3. 선택한 MCS의 소분류(SCS) 목록 조회 (parent_id = MCS.id)
4. 선택한 SCS의 세분류(DCS) 목록 조회 (parent_id = SCS.id)
5. DCS를 최종 선택 → 프롬프트 생성 요청에 포함
```
- 사용자(USER)는 DCS를 최종 선택하여 프롬프트 생성에 사용
- 각 단계별로 `parent_id` 파라미터를 받는 단일 API로 구현 가능

#### 태그 (이미지 스타일)
- **목적**: 생성될 이미지의 **스타일/분위기** 지정
- **구조**: 태그 그룹 → 태그 값들
- **선택 방식**: 태그 그룹별로 단일 선택 또는 복수 선택 가능 여부가 다름
- **예시**:
  ```
  인물    (단일 선택) → 남자, 여자
  분위기  (복수 선택) → 화사한, 추운, 따뜻한, 몽환적, ...
  ```
- **관리**: ADMIN, MANAGER만 CUD 가능
- **조회**: 전체 회원 가능

#### 프롬프트 생성 시 활용
- **카테고리 선택값 + 태그 선택값** 을 모두 Gemini에 전달하여 프롬프트 생성

---

### 3-4. 프롬프트 저장 및 공유

#### 저장 데이터
- 생성된 **프롬프트 텍스트**
- 생성 시 선택한 **카테고리 값**
- 생성 시 선택한 **태그 값들**
- 생성 일시, 생성자

#### 저장 목적
- **히스토리**: 과거 생성 이력 조회
- **재사용**: 성능 좋았던 프롬프트를 팀 내에서 공유하여 재사용

#### 조회 권한
| Role | 조회 범위 |
|------|---------|
| `USER` | 같은 `team_name`을 가진 유저들의 프롬프트 |
| `MANAGER` | 전체 프롬프트 조회 |
| `ADMIN` | 전체 프롬프트 조회 |

---

## 4. DB 설계 원칙

- **ID**: 모든 테이블 PK는 `BIGINT GENERATED ALWAYS AS IDENTITY` (PostgreSQL sequence)
- **FK 제약조건**: 논리적 관계는 ERD로 관리하되, **실제 DB에 FK CONSTRAINT는 생성하지 않음**
  - 참조 무결성은 Service 레이어에서 보장
  - CUD 편의성 및 배치 작업 유연성 확보
- **공통 컬럼 네이밍** (DB: snake_case / Java: camelCase, MyBatis 자동 매핑):
  - `init_regi_pic` → `initRegiPic` — 등록자 (login_id 저장)
  - `init_regi_dttm` → `initRegiDttm` — 등록일시
  - `final_rvis_pic` → `finalRvisPic` — 최종수정자 (login_id 저장)
  - `final_rvis_dttm` → `finalRvisDttm` — 최종수정일시
- **카테고리 계층**: `depth` 숫자 대신 `category_type` 으로 명시 (`LCS` / `MCS` / `SCS` / `DCS`)

---

## 5. 주요 도메인 엔티티 (초안)

```
User
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - login_id          VARCHAR
  - password          VARCHAR (encrypted)
  - name              VARCHAR
  - role              VARCHAR  (ADMIN / MANAGER / USER)
  - team_name         VARCHAR  (FK 제약 없음, 팀명 직접 저장)
  - daily_limit       INT      (하루 생성 가능 횟수, ADMIN이 설정)
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP

Category
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - name              VARCHAR
  - category_type     VARCHAR  (LCS=대분류 / MCS=중분류 / SCS=소분류 / DCS=세분류)
  - parent_id         BIGINT   (self-reference, nullable → LCS는 null)
  - sort_order        INT
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP

TagGroup (태그 그룹)
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - name              VARCHAR  (ex: 인물, 분위기)
  - is_multi_select   BOOLEAN  (복수 선택 가능 여부)
  - sort_order        INT
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP

Tag (태그 값)
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - tag_group_id      BIGINT   (논리 FK → TagGroup, 제약 없음)
  - name              VARCHAR  (ex: 남자, 여자, 화사한)
  - sort_order        INT
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP

PromptHistory (프롬프트 저장)
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - user_id           BIGINT   (논리 FK → User, 제약 없음)
  - category_id BIGINT   (논리 FK → Category 최종 선택값 DCS, 제약 없음)
  - prompt_text       TEXT     (생성된 영문 프롬프트)
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP

PromptHistoryTag (프롬프트-태그 매핑)
  - id                BIGINT GENERATED ALWAYS AS IDENTITY (PK)
  - prompt_history_id BIGINT   (논리 FK → PromptHistory, 제약 없음)
  - tag_id            BIGINT   (논리 FK → Tag, 제약 없음)
  - init_regi_pic     VARCHAR
  - init_regi_dttm    TIMESTAMP
  - final_rvis_pic    VARCHAR
  - final_rvis_dttm   TIMESTAMP
```

---

## 6. 비기능 요구사항

| 항목 | 내용 |
|------|------|
| 배포 환경 | PoC: 로컬 / 운영: GCP · Private Cloud · On-Premise 중 검토 중 |
| 컨테이너 | Dockerfile 추후 생성 예정 |
| API 방식 | REST API |
| 응답 형식 | JSON |
| 인증 방식 | JWT Bearer Token |
| **API URL prefix** | `/api/{domain}` (예: `/api/users`, `/api/categories`, `/api/prompts`) |
| **페이지네이션** | PoC 단계에서는 전체 리스트 한 번에 반환. 추후 필요 시 PageHelper 적용 예정 |
| **공통 응답 래퍼** | `CommonResponse<T>(int code, String message, T data)` — `code`는 HTTP 상태코드 |

---

## 7. 환경변수 / 시크릿 관리

### `.env` 파일 전략
- `.env` 파일은 **`.gitignore`에 추가** — 실제 키 값은 절대 커밋하지 않음
- **`.env.example`** 파일을 키 이름만 빈 값으로 작성하여 커밋 → 팀원이 복사 후 값 채워서 사용

```bash
# .env.example (커밋 O — 키 이름 공유용)
GEMINI_API_KEY=
DB_USER=
DB_PASSWORD=

# .env (커밋 X — 실제 값, .gitignore 처리)
GEMINI_API_KEY=실제키값
DB_USER=실제유저
DB_PASSWORD=실제비밀번호
```

### Spring Profile 전략
Spring Boot는 `.env` 파일을 자동으로 읽지 않음. Spring Profile과 조합하여 환경별로 분리:

| 파일 | 커밋 | 역할 |
|---|:---:|---|
| `application.yml` | O | 공통 설정 + `spring.profiles.active: local` 기본값 |
| `application-local.yml` | O | 로컬 전용 설정 (`${...}` 참조만 사용, 실제 값 없음) |
| `.env` | X | 실제 시크릿 값 (`.gitignore` 처리) |
| `.env.example` | O | 키 이름만 빈 값으로 공유 |

**환경별 실행 방식**
- **로컬**: IDE Run Configuration에서 `.env` 파일 지정 → `${...}` 환경변수 주입
- **Docker/k8s**: `env_file` 옵션 또는 Secret으로 주입, `SPRING_PROFILES_ACTIVE=prod`로 프로파일 오버라이드

---

## 8. 향후 고려 사항 (현재 범위 외)

- [ ] 이미지 생성 AI(nanobanana, Midjourney 등) 직접 API 연동
- [ ] Dockerfile 및 배포 환경 확정
- [ ] 운영 환경 결정 (GCP / Private Cloud / On-Premise)

# CLAUDE.md

이 파일은 Claude Code / Claude AI가 프로젝트를 이해하기 위한 진입점 문서입니다.
코드 작성 전 반드시 이 문서와 `docs/requirements.md` 를 참조하세요.

---

## 프로젝트 개요

**ns-creative-studio-bo** — 이미지 생성형 AI용 프롬프트 자동 생성 서비스의 백엔드 API 서버

사용자가 **상품 카테고리(어떤 상품)** 와 **이미지 스타일 태그(어떻게 보일지)** 를 선택하면,
Gemini API를 통해 최적화된 **영문 이미지 생성 프롬프트**를 자동 생성한다.
생성된 프롬프트는 팀 내에서 공유되어 재사용할 수 있다.

> 이 서버는 프롬프트 텍스트만 생성한다. 실제 이미지 생성(nanobanana, Midjourney 등)은 사용자가 직접 수행한다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle |
| Base Package | `com.nsmall.ns_creative_studio_bo` |
| DB | PostgreSQL |
| ORM | MyBatis |
| 인증 | Spring Security + JWT (Stateless) |
| AI | Gemini API (`gemini-3-flash-preview` 기본, 필요 시 `gemini-3-pro-preview`) |

---

## 빌드 / 실행

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun
```

---

## 패키지 구조

```
com.nsmall.ns_creative_studio_bo
├── domain
│   ├── user
│   ├── category
│   ├── tag
│   └── prompt
├── global
│   ├── config        # Spring Security, MyBatis, Gemini 설정
│   ├── exception     # 공통 예외 처리
│   ├── response      # 공통 API 응답 형식
│   └── util
└── NsCreativeStudioBoApplication.java
```

각 도메인 패키지 내부 구조:
```
{domain}
├── controller
├── service
├── mapper          # MyBatis Mapper 인터페이스
├── dto
│   ├── request
│   └── response
└── entity          # DB 테이블 매핑 객체
```

---

## 핵심 도메인 개념

### 권한(Role) — 계층 구조
```
ADMIN > MANAGER > USER
```
| Role | 주요 권한 |
|------|---------|
| `ADMIN` | 모든 기능 풀 접근, 유저 생성/관리, daily_limit 설정 |
| `MANAGER` | USER 권한 + 카테고리·태그 CUD |
| `USER` | 프롬프트 생성(하루 N회), 같은 팀 프롬프트 조회 |

- 회원가입 없음. ADMIN이 API를 통해 직접 계정 생성
- 팀 개념: `User.team_name` VARCHAR 컬럼으로 관리 (별도 Team 테이블 없음)

### 카테고리 vs 태그 — 완전히 다른 개념

**카테고리** (어떤 상품을 촬영/생성할지)
- 4단계 계층: LCS(대) > MCS(중) > SCS(소) > DCS(세분류)
- self-reference 구조 (`parent_id`)
- ex: `식품 > 건강식품 > 약재 > 인삼`

**태그** (이미지 스타일/분위기)
- TagGroup → Tag 구조
- 그룹별 단일/복수 선택 여부 다름 (`is_multi_select`)
- ex: `인물(단일) → 남자/여자`, `분위기(복수) → 화사한/추운/따뜻한`

### 프롬프트 생성 흐름
```
카테고리 선택 + 태그 선택 → Gemini API → 영문 프롬프트 반환 → PromptHistory 저장
```

### 사용량 체크 (배치 없음)
```
요청 시 PromptHistory COUNT(오늘, 해당 유저) >= User.daily_limit → 거부
```

---

## DB 설계 원칙

- **PK**: `BIGINT GENERATED ALWAYS AS IDENTITY` (sequence)
- **FK 제약조건**: DB에 생성하지 않음. 논리적 관계만 유지, 정합성은 Service에서 보장
- **공통 컬럼**:
  - `INIT_REGI_PIC` / `INIT_REGI_DTTM` — 등록자 / 등록일시
  - `FINAL_RVIS_PIC` / `FINAL_RVIS_DTTM` — 최종수정자 / 최종수정일시
- **카테고리 계층 구분**: `category_type` 컬럼 (`LCS` / `MCS` / `SCS` / `DCS`)

> ⚠️ `createdAt`, `updatedAt` 사용 금지. 반드시 위의 공통 컬럼 네이밍을 사용할 것.

---

## 주요 엔티티 요약

| 엔티티 | 테이블명(예정) | 설명 |
|--------|-------------|------|
| User | USER | 회원 정보, 권한, 팀명, 일일 한도 |
| Category | CATEGORY | 상품 카테고리 계층 (LCS~DCS) |
| TagGroup | TAG_GROUP | 태그 묶음 (인물, 분위기 등) |
| Tag | TAG | 태그 값 (남자, 화사한 등) |
| PromptHistory | PROMPT_HISTORY | 생성된 프롬프트 저장 |
| PromptHistoryTag | PROMPT_HISTORY_TAG | 프롬프트-태그 매핑 |

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](docs/requirements.md)
- 개발 진행 현황: [`docs/todo.md`](docs/todo.md)

# TODO

> 상태: 🟡 개발 준비 단계
> 최종 업데이트: 2026-02-25

---

## 진행 중

없음

---

## 완료

### 문서화
- [x] `docs/requirements.md` — 요구사항 명세 작성
- [x] `CLAUDE.md` — Claude Code / Claude AI 진입점 문서 작성
- [x] `GEMINI.md` — Gemini CLI 진입점 문서 작성
- [x] `.cursor/rules/project-context.mdc` — 프로젝트 도메인 컨텍스트 작성
- [x] `.cursor/rules/coding-convention.mdc` — 코딩 컨벤션 작성
- [x] `docs/todo.md` — 개발 진행 현황 문서 작성

---

## 예정

### Step 1. 로컬 DB 준비
- [ ] `Dockerfile.postgres` 작성 (루트, PostgreSQL 커스텀 이미지)
- [ ] `docker-compose.yml` 작성 (Dockerfile.postgres 참조)
- [ ] `db/schema.sql` 작성 (DDL — 테이블 6개)
- [ ] `db/data.sql` 작성 (초기 데이터 — ADMIN 계정, 기초 카테고리/태그)
- [ ] 로컬 PostgreSQL 컨테이너 실행 확인

### Step 2. 프로젝트 의존성 / 설정
- [ ] `build.gradle` 의존성 추가
  - [ ] `spring-boot-starter-security` 주석 해제
  - [ ] `mybatis-spring-boot-starter` 주석 해제 (Spring Boot 3.5.x 호환 버전 확인)
  - [ ] `postgresql` 드라이버 주석 해제
  - [ ] JWT 라이브러리 추가 (`jjwt` 등)
  - [ ] Gemini SDK 추가
- [ ] `src/main/resources/mybatis-config.xml` 작성 (`mapUnderscoreToCamelCase=true` 포함)
- [ ] `src/main/resources/application.yml` 기본 설정 작성 (DB 연결, 포트 등)

### Step 3. 기능 개발
- [ ] 회원 / 인증 (User, JWT, Spring Security)
- [ ] 카테고리 CRUD (Category)
- [ ] 태그 그룹 / 태그 CRUD (TagGroup, Tag)
- [ ] 프롬프트 생성 (Gemini API 연동)
- [ ] 프롬프트 히스토리 저장 / 조회

---

## 참고 문서

- 전체 요구사항: [`docs/requirements.md`](requirements.md)
- 프로젝트 진입점: [`CLAUDE.md`](../CLAUDE.md)

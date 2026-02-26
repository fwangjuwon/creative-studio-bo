-- =============================================================================
-- ns-creative-studio-bo DDL
-- 기반 문서: docs/erd.md
-- DB: PostgreSQL 15
--
-- 설계 원칙:
--   - PK: BIGINT GENERATED ALWAYS AS IDENTITY
--   - FK CONSTRAINT 생성하지 않음 (논리 FK만 주석으로 명시)
--   - 모든 테이블에 공통 감사 컬럼 4개 포함
--     (init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm)
-- =============================================================================


-- -----------------------------------------------------------------------------
-- TABLE: user
-- 설명: 회원 정보, 권한(ADMIN/MANAGER/USER), 팀명, 일일 생성 한도
-- 논리 FK: prompt_history.user_id → user.id
-- -----------------------------------------------------------------------------
CREATE TABLE "user" (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id        VARCHAR(50)     NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,           -- ADMIN / MANAGER / USER
    team_name       VARCHAR(100),
    daily_limit     INT             NOT NULL DEFAULT 10,
    init_regi_pic   VARCHAR(50),
    init_regi_dttm  TIMESTAMP,
    final_rvis_pic  VARCHAR(50),
    final_rvis_dttm TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- TABLE: category
-- 설명: 상품 카테고리 4단계 계층 (LCS → MCS → SCS → DCS)
--       self-reference 구조로 관리. LCS는 parent_id = NULL
-- 논리 FK: category.parent_id → category.id (self-ref)
--          prompt_history.category_id → category.id
-- -----------------------------------------------------------------------------
CREATE TABLE category (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    category_type   VARCHAR(10)     NOT NULL,           -- LCS / MCS / SCS / DCS
    parent_id       BIGINT,                             -- 논리 FK → category.id (LCS는 NULL)
    sort_order      INT             NOT NULL DEFAULT 0,
    init_regi_pic   VARCHAR(50),
    init_regi_dttm  TIMESTAMP,
    final_rvis_pic  VARCHAR(50),
    final_rvis_dttm TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- TABLE: tag_group
-- 설명: 태그 묶음 (인물, 분위기 등)
--       is_multi_select로 단일/복수 선택 여부 관리
-- 논리 FK: tag.tag_group_id → tag_group.id
-- -----------------------------------------------------------------------------
CREATE TABLE tag_group (
    id               BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(100)    NOT NULL,
    is_multi_select  BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order       INT             NOT NULL DEFAULT 0,
    init_regi_pic    VARCHAR(50),
    init_regi_dttm   TIMESTAMP,
    final_rvis_pic   VARCHAR(50),
    final_rvis_dttm  TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- TABLE: tag
-- 설명: 태그 값 (남자, 여자, 화사한 등)
-- 논리 FK: tag.tag_group_id → tag_group.id
--          prompt_history_tag.tag_id → tag.id
-- -----------------------------------------------------------------------------
CREATE TABLE tag (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tag_group_id    BIGINT          NOT NULL,           -- 논리 FK → tag_group.id
    name            VARCHAR(100)    NOT NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    init_regi_pic   VARCHAR(50),
    init_regi_dttm  TIMESTAMP,
    final_rvis_pic  VARCHAR(50),
    final_rvis_dttm TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- TABLE: prompt_history
-- 설명: Gemini가 생성한 영문 프롬프트 저장
--       init_regi_dttm 기준으로 일일 사용량 집계 (DATE(init_regi_dttm) = 오늘)
-- 논리 FK: prompt_history.user_id → user.id
--          prompt_history.category_id → category.id (DCS만 저장)
-- -----------------------------------------------------------------------------
CREATE TABLE prompt_history (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT          NOT NULL,           -- 논리 FK → user.id
    category_id     BIGINT          NOT NULL,           -- 논리 FK → category.id (DCS)
    prompt_text     TEXT            NOT NULL,
    init_regi_pic   VARCHAR(50),
    init_regi_dttm  TIMESTAMP,
    final_rvis_pic  VARCHAR(50),
    final_rvis_dttm TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- TABLE: prompt_history_tag
-- 설명: 프롬프트와 태그의 다대다 매핑
-- 논리 FK: prompt_history_tag.prompt_history_id → prompt_history.id
--          prompt_history_tag.tag_id → tag.id
-- -----------------------------------------------------------------------------
CREATE TABLE prompt_history_tag (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prompt_history_id   BIGINT      NOT NULL,           -- 논리 FK → prompt_history.id
    tag_id              BIGINT      NOT NULL,           -- 논리 FK → tag.id
    init_regi_pic       VARCHAR(50),
    init_regi_dttm      TIMESTAMP,
    final_rvis_pic      VARCHAR(50),
    final_rvis_dttm     TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- INDEX
-- -----------------------------------------------------------------------------

-- 일일 사용량 집계 쿼리 최적화
-- WHERE user_id = ? AND DATE(init_regi_dttm) = 오늘
CREATE INDEX idx_prompt_history_user_date
    ON prompt_history (user_id, init_regi_dttm);

-- 카테고리 단계별 드릴다운 조회 최적화
-- WHERE parent_id = ? ORDER BY sort_order
CREATE INDEX idx_category_parent_id
    ON category (parent_id);

-- 태그 그룹별 태그 조회 최적화
-- WHERE tag_group_id = ? ORDER BY sort_order
CREATE INDEX idx_tag_tag_group_id
    ON tag (tag_group_id);

-- 프롬프트-태그 매핑 조회 최적화
CREATE INDEX idx_prompt_history_tag_history_id
    ON prompt_history_tag (prompt_history_id);

-- =============================================================================
-- ns-creative-studio-bo 초기 데이터
-- 기반 문서: docs/erd.md
--
-- 주의: 이 파일은 docker-entrypoint-initdb.d/ 에 마운트되어
--       PostgreSQL 컨테이너 볼륨 최초 생성 시 1회만 실행됩니다.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- ADMIN 계정
-- login_id : admin
-- -----------------------------------------------------------------------------
INSERT INTO "user" (
    login_id,
    password,
    name,
    role,
    daily_limit,
    init_regi_pic,
    init_regi_dttm,
    final_rvis_pic,
    final_rvis_dttm
) VALUES (
    'admin',
    '$2y$10$ri4C2IVNz4A96dNtBD5uK.8./he/tMZyJZFTNz8llCXEXxjr7Hi7y',
    '관리자',
    'ADMIN',
    999,
    'system',
    NOW(),
    'system',
    NOW()
);

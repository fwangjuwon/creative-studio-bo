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


-- =============================================================================
-- 인간 이미지 프롬프트 생성 — 시드 데이터
-- 5단계: A(핵심 피사체) / B(분위기·스타일) / C(리얼리즘) / D(조명·배경) / E(촬영기술)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- STEP 데이터
-- -----------------------------------------------------------------------------
INSERT INTO human_prompt_step (step_number, step_code, name, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
(1, 'A', '핵심 피사체 정의',   'system', NOW(), 'system', NOW()),
(2, 'B', '분위기 및 스타일',   'system', NOW(), 'system', NOW()),
(3, 'C', '리얼리즘 디테일',    'system', NOW(), 'system', NOW()),
(4, 'D', '조명 및 배경',       'system', NOW(), 'system', NOW()),
(5, 'E', '촬영 기술/스타일',   'system', NOW(), 'system', NOW());


-- -----------------------------------------------------------------------------
-- ATTR 데이터 (step_id는 위에서 생성된 순서대로 1~5)
-- -----------------------------------------------------------------------------

-- Step A (step_id=1): 핵심 피사체 정의
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '연령대',    FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '성별',      FALSE, TRUE,  2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '국적/인종', FALSE, FALSE, 3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '인원수',    FALSE, TRUE,  4, 'system', NOW(), 'system', NOW());

-- Step B (step_id=2): 분위기 및 스타일
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '전반적 분위기', FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '표정',          FALSE, FALSE, 2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '시선 처리',     FALSE, FALSE, 3, 'system', NOW(), 'system', NOW());

-- Step C (step_id=3): 리얼리즘 디테일
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'C'), '피부 질감', TRUE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'C'), '기타 요소', TRUE, FALSE, 2, 'system', NOW(), 'system', NOW());

-- Step D (step_id=4): 조명 및 배경
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'D'), '조명',      FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'D'), '배경 장소', FALSE, TRUE,  2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'D'), '배경 처리', FALSE, FALSE, 3, 'system', NOW(), 'system', NOW());

-- Step E (step_id=5): 촬영 기술/스타일
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'E'), '프레임',      FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'E'), '카메라 느낌', FALSE, FALSE, 2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'E'), '강조 필터',   TRUE,  FALSE, 3, 'system', NOW(), 'system', NOW());


-- -----------------------------------------------------------------------------
-- OPTION 데이터 (attr_id는 name으로 서브쿼리 참조)
-- -----------------------------------------------------------------------------

-- A-1: 연령대 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '영유아',    'Baby/Toddler',         1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '어린이',    'Child',                2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '청소년',    'Teenager',             3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '20대 초반', 'Early 20s',            4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '20대 후반', 'Late 20s',             5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '30~40대',   'Adult 30s-40s',        6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '중년',      'Middle-aged 50s',      7, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '연령대'), '노년',      'Senior/Elderly 60s+',  8, 'system', NOW(), 'system', NOW());

-- A-2: 성별 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '성별'), '여성',     'Woman',              1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '성별'), '남성',     'Man',                2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '성별'), '중성적인', 'Androgynous person', 3, 'system', NOW(), 'system', NOW());

-- A-3: 국적/인종 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '한국인',     'Korean',                  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '동아시아인', 'East Asian',              2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '동남아시아인','Southeast Asian',        3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '백인',       'Caucasian/White',         4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '흑인',       'Black/African descent',   5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '히스패닉',   'Hispanic/Latino',         6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '혼혈',       'Mixed race',              7, 'system', NOW(), 'system', NOW());

-- A-4: 인원수 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '인원수'), '1인 단독',     'Solo portrait',         1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '인원수'), '커플/2인',     'Couple',                2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '인원수'), '소규모 그룹',  'Small group of friends',3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '인원수'), '가족',         'Family portrait',       4, 'system', NOW(), 'system', NOW());

-- B-1: 전반적 분위기 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '전반적 분위기'), '자연스러운 일상', 'Natural everyday candid',        1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '전반적 분위기'), '영화같은',        'Cinematic mood',                 2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '전반적 분위기'), '전문적인/프로필', 'Professional LinkedIn headshot', 3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '전반적 분위기'), '패션 화보',       'Editorial fashion',              4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '전반적 분위기'), '몽환/감성',       'Dreamy/Moody',                   5, 'system', NOW(), 'system', NOW());

-- B-2: 표정 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '무표정/시크함', 'Neutral expression',      1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '옅은 미소',     'Soft natural smile',      2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '활짝 웃음',     'Big laughing smile',      3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '진지함',        'Intense/Serious',         4, 'system', NOW(), 'system', NOW());

-- B-3: 시선 처리 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '카메라 정면 응시', 'Looking directly at camera',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '먼 곳 응시',       'Looking off-camera/Candid gaze',2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '눈을 내리깜',      'Looking down',                  3, 'system', NOW(), 'system', NOW());

-- C-1: 피부 질감 (복수선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '실제 피부 질감/모공', 'Real human skin texture with visible pores', 1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '주근깨/기미',         'Freckles/Sunspots',                          2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '잔주름',              'Natural fine lines/Wrinkles',                3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '결점/잡티',           'Slight skin imperfections',                  4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '땀/유분기',           'Slight sweat/Dewy skin',                     5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '피부 질감'), '수염',                'Stubble/Beard texture',                      6, 'system', NOW(), 'system', NOW());

-- C-2: 기타 요소 (복수선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '기타 요소'), '얼굴 비대칭',      'Realistic facial asymmetry',      1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '기타 요소'), '화장기 없는',      'Barely-there makeup/No makeup',   2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '기타 요소'), '바람에 날린 머릿결','Wind-blown messy hair',           3, 'system', NOW(), 'system', NOW());

-- D-1: 조명 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '부드러운 자연광',  'Soft natural daylight',   1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '황금시간대/노을',  'Golden hour lighting',    2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '창문 빛',          'Window light',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '강렬한 직사광선',  'Harsh sunlight',          4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '어두운/저조도',    'Dim low light',           5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '네온사인',         'Neon city lights',        6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '스튜디오 조명',    'Studio strobe lighting',  7, 'system', NOW(), 'system', NOW());

-- D-2: 배경 장소 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '집 내부/방',     'Home interior/Cozy room',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '카페/식당',      'Cafe/Restaurant',            2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '도심 거리',      'Urban street/Cityscape',     3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '공원/자연',      'Park/Nature outdoor',        4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '단색 스튜디오',  'Plain neutral studio wall',  5, 'system', NOW(), 'system', NOW());

-- D-3: 배경 처리 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '배경 처리'), '배경 흐림/보케',  'Blurred background/Bokeh',   1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 처리'), '배경 선명하게',   'Sharp background details',   2, 'system', NOW(), 'system', NOW());

-- E-1: 프레임 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '얼굴 클로즈업', 'Extreme close-up face',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '가슴 위',       'Bust shot/Headshot',       2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '허리 위',       'Waist-up shot',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '전신',          'Full body shot',           4, 'system', NOW(), 'system', NOW());

-- E-2: 카메라 느낌 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '스마트폰 셀카',    'Smartphone front camera selfie',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), 'DSLR 고화질',      'High-resolution DSLR photo',        2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '필름 카메라',      'Analog film grain 35mm',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '폴라로이드/빈티지','Polaroid/Vintage photo style',      4, 'system', NOW(), 'system', NOW());

-- E-3: 강조 필터 (복수선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '강조 필터'), '보정 없는 사실주의', 'Unfiltered photorealism',         1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '강조 필터'), '뷰티 필터 금지',     'No beauty filter/Not airbrushed', 2, 'system', NOW(), 'system', NOW());

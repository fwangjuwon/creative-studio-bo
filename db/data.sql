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
    team_name,
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
    'IT개발팀',
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
-- STEP 데이터 (총 3단계)
-- 개별(is_common=false): Step 1(A) — 핵심 피사체 정의 (인물별 개별 선택)
-- 공통(is_common=true): Step 2(B), 3(C) — 조명/배경, 촬영기술
-- -----------------------------------------------------------------------------
INSERT INTO human_prompt_step (step_number, step_code, name, is_common, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
(1, 'A', '핵심 피사체 정의',   FALSE, 'system', NOW(), 'system', NOW()),
(2, 'B', '조명 및 배경',       TRUE,  'system', NOW(), 'system', NOW()),
(3, 'C', '촬영 기술/스타일',   TRUE,  'system', NOW(), 'system', NOW());


-- -----------------------------------------------------------------------------
-- ATTR 데이터
-- -----------------------------------------------------------------------------

-- Step A (step_number=1, is_common=false): 핵심 피사체 정의
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '연령대',      FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '성별',        FALSE, TRUE,  2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '국적/인종',   FALSE, FALSE, 3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '표정',        FALSE, FALSE, 4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '시선 처리',   FALSE, FALSE, 5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '의상 스타일', FALSE, FALSE, 6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '헤어스타일',  FALSE, FALSE, 7, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '헤어 컬러',   FALSE, FALSE, 8, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'A'), '체형',        FALSE, FALSE, 9, 'system', NOW(), 'system', NOW());

-- Step B (step_number=2, is_common=true): 조명 및 배경
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '조명',      FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '배경 장소', FALSE, TRUE,  2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'B'), '배경 처리', FALSE, FALSE, 3, 'system', NOW(), 'system', NOW());

-- Step C (step_number=3, is_common=true): 촬영 기술/스타일
INSERT INTO human_prompt_attr (step_id, name, is_multi_select, is_required, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_step WHERE step_code = 'C'), '프레임',      FALSE, TRUE,  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'C'), '카메라 느낌', FALSE, FALSE, 2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_step WHERE step_code = 'C'), '강조 필터',   TRUE,  FALSE, 3, 'system', NOW(), 'system', NOW());


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
((SELECT id FROM human_prompt_attr WHERE name = '성별'), '여성', 'Woman', 1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '성별'), '남성', 'Man',   2, 'system', NOW(), 'system', NOW());

-- A-3: 국적/인종 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '한국인',     'Korean',                  1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '동아시아인', 'East Asian',              2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '동남아시아인','Southeast Asian',        3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '백인',       'Caucasian/White',         4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '흑인',       'Black/African descent',   5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '히스패닉',   'Hispanic/Latino',         6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '국적/인종'), '혼혈',       'Mixed race',              7, 'system', NOW(), 'system', NOW());


-- A-4: 표정 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '무표정/시크함', 'Neutral/Expressionless',   1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '옅은 미소',     'Soft natural smile',       2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '활짝 웃음',     'Big laughing smile',       3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '표정'), '진지함',        'Intense/Serious look',     4, 'system', NOW(), 'system', NOW());

-- A-5: 시선 처리 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '카메라 정면 응시', 'Looking directly at camera',     1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '먼 곳 응시',       'Looking off-camera/Candid gaze', 2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '시선 처리'), '눈을 내리깜',      'Looking down/Eyes cast down',   3, 'system', NOW(), 'system', NOW());

-- A-6: 의상 스타일 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '의상 스타일'), '캐주얼',        'Casual everyday outfit',          1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '의상 스타일'), '포멀/비즈니스', 'Formal business attire',          2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '의상 스타일'), '스트릿 패션',   'Street fashion/Urban style',      3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '의상 스타일'), '스포츠웨어',    'Sportswear/Athletic wear',        4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '의상 스타일'), '패션 화보풍',   'Editorial high-fashion outfit',   5, 'system', NOW(), 'system', NOW());

-- A-7: 헤어스타일 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '헤어스타일'), '긴 생머리',  'Long straight hair',        1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어스타일'), '단발',       'Bob cut/Short bob',         2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어스타일'), '업스타일',   'Updo/Hair tied up',         3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어스타일'), '짧은 머리',  'Short hair/Crew cut',       4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어스타일'), '웨이브/곱슬','Wavy/Curly hair',           5, 'system', NOW(), 'system', NOW());

-- A-8: 헤어 컬러 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '헤어 컬러'), '검정',      'Black hair',              1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어 컬러'), '갈색',      'Brown hair',              2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어 컬러'), '금발',      'Blonde hair',             3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어 컬러'), '밝은 염색', 'Bright colored hair/Dyed',4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '헤어 컬러'), '회색/백발', 'Grey/Silver/White hair',  5, 'system', NOW(), 'system', NOW());

-- A-9: 체형 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '체형'), '날씬한',      'Slim/Slender build',     1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '체형'), '보통',        'Average/Normal build',   2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '체형'), '건장한/근육질','Athletic/Muscular build', 3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '체형'), '풍만한',      'Curvy/Full-figured',     4, 'system', NOW(), 'system', NOW());

-- B, C 단계 옵션 제거됨

-- B-1: 조명 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '부드러운 자연광',  'Soft natural daylight',   1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '황금시간대/노을',  'Golden hour lighting',    2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '창문 빛',          'Window light',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '강렬한 직사광선',  'Harsh sunlight',          4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '어두운/저조도',    'Dim low light',           5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '네온사인',         'Neon city lights',        6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '조명'), '스튜디오 조명',    'Studio strobe lighting',  7, 'system', NOW(), 'system', NOW());

-- B-2: 배경 장소 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '집 내부/방',     'Home interior/Cozy room',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '카페/식당',      'Cafe/Restaurant',            2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '도심 거리',      'Urban street/Cityscape',     3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '공원/자연',      'Park/Nature outdoor',        4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 장소'), '단색 스튜디오',  'Plain neutral studio wall',  5, 'system', NOW(), 'system', NOW());

-- B-3: 배경 처리 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '배경 처리'), '배경 흐림/보케',  'Blurred background/Bokeh',   1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '배경 처리'), '배경 선명하게',   'Sharp background details',   2, 'system', NOW(), 'system', NOW());

-- C-1: 프레임 (단일선택, 필수)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '얼굴 클로즈업', 'Extreme close-up face',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '가슴 위',       'Bust shot/Headshot',       2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '허리 위',       'Waist-up shot',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '프레임'), '전신',          'Full body shot',           4, 'system', NOW(), 'system', NOW());

-- C-2: 카메라 느낌 (단일선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '스마트폰 셀카',    'Smartphone front camera selfie',    1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), 'DSLR 고화질',      'High-resolution DSLR photo',        2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '필름 카메라',      'Analog film grain 35mm',            3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '카메라 느낌'), '폴라로이드/빈티지','Polaroid/Vintage photo style',      4, 'system', NOW(), 'system', NOW());

-- C-3: 강조 필터 (복수선택, 선택)
INSERT INTO human_prompt_option (attr_id, name, prompt_value, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM human_prompt_attr WHERE name = '강조 필터'), '보정 없는 사실주의', 'Unfiltered photorealism',         1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM human_prompt_attr WHERE name = '강조 필터'), '뷰티 필터 금지',     'No beauty filter/Not airbrushed', 2, 'system', NOW(), 'system', NOW());

-- =============================================================================
-- 상품 카테고리 시드 데이터 (LCS → MCS → SCS)
-- =============================================================================

-- LCS (대분류)
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('식품',   'LCS', NULL, 1, 'system', NOW(), 'system', NOW()),
('비식품', 'LCS', NULL, 2, 'system', NOW(), 'system', NOW()),
('무형',   'LCS', NULL, 3, 'system', NOW(), 'system', NOW());

-- MCS (중분류) — 식품
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('건강식품', 'MCS', (SELECT id FROM category WHERE name = '식품'   AND category_type = 'LCS'), 1, 'system', NOW(), 'system', NOW()),
('일반식품', 'MCS', (SELECT id FROM category WHERE name = '식품'   AND category_type = 'LCS'), 2, 'system', NOW(), 'system', NOW()),
('가공식품', 'MCS', (SELECT id FROM category WHERE name = '식품'   AND category_type = 'LCS'), 3, 'system', NOW(), 'system', NOW()),
('기타',     'MCS', (SELECT id FROM category WHERE name = '식품'   AND category_type = 'LCS'), 4, 'system', NOW(), 'system', NOW());

-- MCS (중분류) — 비식품
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('생활', 'MCS', (SELECT id FROM category WHERE name = '비식품' AND category_type = 'LCS'), 1, 'system', NOW(), 'system', NOW()),
('뷰티', 'MCS', (SELECT id FROM category WHERE name = '비식품' AND category_type = 'LCS'), 2, 'system', NOW(), 'system', NOW()),
('패션', 'MCS', (SELECT id FROM category WHERE name = '비식품' AND category_type = 'LCS'), 3, 'system', NOW(), 'system', NOW());

-- MCS (중분류) — 무형
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('상담/예약', 'MCS', (SELECT id FROM category WHERE name = '무형' AND category_type = 'LCS'), 1, 'system', NOW(), 'system', NOW()),
('판매',      'MCS', (SELECT id FROM category WHERE name = '무형' AND category_type = 'LCS'), 2, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 건강식품
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('건강기능', 'SCS', (SELECT id FROM category WHERE name = '건강식품' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('건강보조', 'SCS', (SELECT id FROM category WHERE name = '건강식품' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('특수목적', 'SCS', (SELECT id FROM category WHERE name = '건강식품' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 일반식품
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('농산물', 'SCS', (SELECT id FROM category WHERE name = '일반식품' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('수산물', 'SCS', (SELECT id FROM category WHERE name = '일반식품' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('축산물', 'SCS', (SELECT id FROM category WHERE name = '일반식품' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 가공식품
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('밀키트',      'SCS', (SELECT id FROM category WHERE name = '가공식품' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('조미료/장류', 'SCS', (SELECT id FROM category WHERE name = '가공식품' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('면류',        'SCS', (SELECT id FROM category WHERE name = '가공식품' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW()),
('베이커리',    'SCS', (SELECT id FROM category WHERE name = '가공식품' AND category_type = 'MCS'), 4, 'system', NOW(), 'system', NOW()),
('유제품',      'SCS', (SELECT id FROM category WHERE name = '가공식품' AND category_type = 'MCS'), 5, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 기타
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('음료', 'SCS', (SELECT id FROM category WHERE name = '기타' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 생활
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('리빙',         'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('주방',         'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('가전',         'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW()),
('가구',         'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 4, 'system', NOW(), 'system', NOW()),
('침구',         'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 5, 'system', NOW(), 'system', NOW()),
('건강/의료기기','SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 6, 'system', NOW(), 'system', NOW()),
('패브릭',       'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 7, 'system', NOW(), 'system', NOW()),
('인테리어 소품','SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 8, 'system', NOW(), 'system', NOW()),
('스포츠',       'SCS', (SELECT id FROM category WHERE name = '생활' AND category_type = 'MCS'), 9, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 뷰티
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('기초',         'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('색조',         'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('헤어',         'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW()),
('바디',         'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 4, 'system', NOW(), 'system', NOW()),
('SUN',          'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 5, 'system', NOW(), 'system', NOW()),
('향수',         'SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 6, 'system', NOW(), 'system', NOW()),
('기기 디바이스','SCS', (SELECT id FROM category WHERE name = '뷰티' AND category_type = 'MCS'), 7, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 패션
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('의류',     'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('이너웨어', 'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('쥬얼리',   'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW()),
('가방',     'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 4, 'system', NOW(), 'system', NOW()),
('신발',     'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 5, 'system', NOW(), 'system', NOW()),
('잡화',     'SCS', (SELECT id FROM category WHERE name = '패션' AND category_type = 'MCS'), 6, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 상담/예약
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('여행', 'SCS', (SELECT id FROM category WHERE name = '상담/예약' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW()),
('보험', 'SCS', (SELECT id FROM category WHERE name = '상담/예약' AND category_type = 'MCS'), 2, 'system', NOW(), 'system', NOW()),
('렌탈', 'SCS', (SELECT id FROM category WHERE name = '상담/예약' AND category_type = 'MCS'), 3, 'system', NOW(), 'system', NOW()),
('상조', 'SCS', (SELECT id FROM category WHERE name = '상담/예약' AND category_type = 'MCS'), 4, 'system', NOW(), 'system', NOW());

-- SCS (소분류) — 판매
INSERT INTO category (name, category_type, parent_id, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('호텔/리조트 숙박권', 'SCS', (SELECT id FROM category WHERE name = '판매' AND category_type = 'MCS'), 1, 'system', NOW(), 'system', NOW());


-- =============================================================================
-- 상품 프롬프트 태그 그룹 시드 데이터
-- =============================================================================

-- TAG GROUP
INSERT INTO tag_group (name, is_multi_select, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('장면',       FALSE, 1, 'system', NOW(), 'system', NOW()),
('카메라 앵글', FALSE, 2, 'system', NOW(), 'system', NOW()),
('조명',       FALSE, 3, 'system', NOW(), 'system', NOW()),
('분위기',     TRUE,  4, 'system', NOW(), 'system', NOW());

-- 장면 (단일선택) — 상품이 놓인 배경/환경
INSERT INTO tag (tag_group_id, name, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM tag_group WHERE name = '장면'), '흰 배경',               1,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '라이프스타일',           2,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '주방/식탁',              3,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '욕실',                   4,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '침실',                   5,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '야외/자연',              6,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '모던 인테리어',          7,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '대리석',                 8,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '원목',                   9,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '수면 위(Water Surface)', 10, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '리넨',                   11, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '기하학적 오브제',        12, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '모래',                   13, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '콘크리트',               14, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '미니멀 데스크',          15, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '벨벳',                   16, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '장면'), '크로마 키',              17, 'system', NOW(), 'system', NOW());

-- 카메라 앵글 (단일선택) — 촬영 각도
INSERT INTO tag (tag_group_id, name, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '로우 앵글',        1, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '정면',              2, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '탑뷰',              3, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '45도',              4, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '사이드뷰',          5, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '클로즈업',          6, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '광각',              7, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '카메라 앵글'), '익스트림 클로즈업', 8, 'system', NOW(), 'system', NOW());

-- 조명 (단일선택) — 광원 스타일
INSERT INTO tag (tag_group_id, name, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM tag_group WHERE name = '조명'), '자연광',          1,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '스튜디오',        2,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '소프트박스',      3,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '역광',            4,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '드라마틱',        5,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '프리즘 무지개',   6,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '네온조명',        7,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '안개광',          8,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '블라인드 그림자', 9,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '골든아워',        10, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '림 라이트',       11, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '나뭇잎 그림자',   12, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '조명'), '스포트라이트',    13, 'system', NOW(), 'system', NOW());

-- 분위기 (복수선택) — 이미지 전체 톤/무드
INSERT INTO tag (tag_group_id, name, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
((SELECT id FROM tag_group WHERE name = '분위기'), '미니멀',        1,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '럭셔리',        2,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '내추럴',        3,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '따뜻한',        4,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '화사한',        5,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '모던',          6,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '빈티지',        7,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '뮤트(차분함)',  8,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '시네마틱',      9,  'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '하이키',        10, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '오가닉',        11, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '사이버펑크',    12, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '비비드',        13, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '미스테리',      14, 'system', NOW(), 'system', NOW()),
((SELECT id FROM tag_group WHERE name = '분위기'), '로우키(Low-key)', 15, 'system', NOW(), 'system', NOW());

-- =============================================================================
-- 이미지 사이즈 태그 그룹 (상품/인물 공통 — FO에서 이미지 비율 선택용)
-- =============================================================================
INSERT INTO tag_group (name, is_multi_select, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm) VALUES
('이미지 사이즈', FALSE, 99, 'system', NOW(), 'system', NOW());

INSERT INTO tag (tag_group_id, name, sort_order, init_regi_pic, init_regi_dttm, final_rvis_pic, final_rvis_dttm, tag_value) VALUES
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '1:1',        1, 'system', NOW(), 'system', NOW(), NULL),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '4:3',        2, 'system', NOW(), 'system', NOW(), NULL),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '16:9',       3, 'system', NOW(), 'system', NOW(), NULL),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '9:16',       4, 'system', NOW(), 'system', NOW(), NULL),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '통강조',     5, 'system', NOW(), 'system', NOW(), '365:825'),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '미디어월80', 6, 'system', NOW(), 'system', NOW(), '9216:1728'),
((SELECT id FROM tag_group WHERE name = '이미지 사이즈'), '미디어월150',7, 'system', NOW(), 'system', NOW(), '12288:1728');

package com.nsmall.ns_creative_studio_bo.domain.category;

/**
 * 카테고리 계층 타입 enum
 *
 * <pre>
 * LCS(대분류) > MCS(중분류) > SCS(소분류) > DCS(세분류)
 * category 테이블의 category_type 컬럼 값으로 저장
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public enum CategoryType {
    LCS, // 대분류
    MCS, // 중분류
    SCS, // 소분류
    DCS  // 세분류
}

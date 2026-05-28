package com.nsmall.ns_creative_studio_bo.domain.prompt;

/**
 * 프롬프트 종류 enum
 *
 * <pre>
 * prompt_history 테이블의 prompt_type 컬럼 값으로 저장된다.
 * PRODUCT — 상품 프롬프트 (카테고리 기반 생성)
 * PERSON  — 인물 프롬프트 (Step/Attr/Option 기반 생성)
 * FUSION  — 합성 이미지 생성 (상품+인물 이미지 기반)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-04-16
 */
public enum PromptType {
    PRODUCT, // 상품 프롬프트
    PERSON,  // 인물 프롬프트
    FUSION   // 합성 이미지 (상품+인물)
}

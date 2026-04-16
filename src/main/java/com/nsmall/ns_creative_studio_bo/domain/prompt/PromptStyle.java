package com.nsmall.ns_creative_studio_bo.domain.prompt;

/**
 * 프롬프트 스타일 enum
 *
 * <pre>
 * prompt_history 테이블의 prompt_style 컬럼 값으로 저장된다.
 * NANO_BANANA — 문장형 (NanoBanana/Gemini 최적화, 기본값)
 * MID_JOURNEY — 키워드 나열형 (MidJourney 최적화)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-04-16
 */
public enum PromptStyle {
    NANO_BANANA, // 문장형 (NanoBanana/Gemini 최적화, 기본값)
    MID_JOURNEY  // 키워드 나열형 (MidJourney 최적화)
}

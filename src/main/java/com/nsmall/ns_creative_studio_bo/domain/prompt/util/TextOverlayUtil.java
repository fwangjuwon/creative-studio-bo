package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

/**
 * 이미지 텍스트 오버레이 Gemini 지시사항 생성 유틸리티
 *
 * <pre>
 * 텍스트 내용만 받아 AI가 이미지 분위기에 맞게 자동으로 스타일/위치/크기를 결정하도록 지시한다.
 * textContent가 null/blank이면 null을 반환하여 텍스트 오버레이 없이 처리한다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-26
 */
public class TextOverlayUtil {

    private TextOverlayUtil() {}

    /**
     * 텍스트 오버레이 suffix 문자열을 생성한다.
     * AI가 이미지 분위기에 맞게 텍스트를 자연스럽게 통합하도록 지시한다.
     *
     * @param textContent 삽입할 텍스트 내용 (null/blank이면 null 반환)
     * @return 프롬프트 suffix 문자열, textContent 없으면 null
     */
    public static String buildSuffix(String textContent) {
        if (textContent == null || textContent.isBlank()) return null;

        return "Naturally integrate the text \"" + textContent.strip() + "\" into the image," +
               " ensuring the exact spelling is rendered correctly and remains highly legible." +
               " Choose the font style, size, placement, and color that best complements the overall mood and composition.";
    }
}

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
     *
     * <pre>
     * textContent만  → 메인 텍스트를 이미지 분위기에 맞게 자연스럽게 삽입
     * disclaimerText만 → 이미지 내 기존 메인 텍스트 아래 작은 단서 문구로 삽입
     * 둘 다           → 메인 텍스트 삽입 후 단서 문구를 그 바로 아래에 배치
     * 둘 다 없음      → null 반환
     * </pre>
     *
     * @param textContent    삽입할 메인 텍스트 (null/blank 허용)
     * @param disclaimerText 단서 문구 텍스트 (null/blank 허용)
     * @return 프롬프트 suffix 문자열, 둘 다 없으면 null
     */
    public static String buildSuffix(String textContent, String disclaimerText) {
        boolean hasMain       = textContent    != null && !textContent.isBlank();
        boolean hasDisclaimer = disclaimerText != null && !disclaimerText.isBlank();

        if (!hasMain && !hasDisclaimer) return null;

        StringBuilder sb = new StringBuilder();

        if (hasMain) {
            String stripped = textContent.strip();
            String display  = stripped.replace("\n", " / ");
            String hint     = stripped.contains("\n")
                    ? " Each ' / ' in the text represents a line break — render each segment on a separate line."
                    : "";
            sb.append("Naturally integrate the text \"").append(display).append("\" into the image,")
              .append(hint)
              .append(" ensuring the exact spelling is rendered correctly and remains highly legible.")
              .append(" Choose the font style, size, placement, and color that best complements the overall mood and composition.");
        }

        if (hasDisclaimer) {
            if (sb.length() > 0) sb.append(" ");
            String stripped = disclaimerText.strip();
            String display  = stripped.replace("\n", " / ");
            String hint     = stripped.contains("\n")
                    ? " Each ' / ' represents a line break — render each segment on a separate line."
                    : "";
            if (hasMain) {
                sb.append("Also add the disclaimer text \"").append(display).append("\" as a small fine-print disclaimer")
                  .append(hint)
                  .append(" positioned directly below the main text just added,");
            } else {
                sb.append("Add the text \"").append(display).append("\" as a small fine-print disclaimer")
                  .append(hint)
                  .append(" positioned below the main text elements in the image,");
            }
            sb.append(" using a font size noticeably smaller than the headline or main title text,")
              .append(" keeping it subtle yet clearly legible — similar to a product notice or legal disclaimer.")
              .append(" Ensure the exact spelling is rendered correctly.");
        }

        return sb.toString();
    }

    /** {@link #buildSuffix(String, String)} — disclaimerText 없는 단축 오버로드 */
    public static String buildSuffix(String textContent) {
        return buildSuffix(textContent, (String) null);
    }
}

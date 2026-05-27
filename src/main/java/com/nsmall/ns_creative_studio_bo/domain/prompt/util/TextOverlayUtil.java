package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

/**
 * 이미지 텍스트 오버레이 Gemini 지시사항 생성 유틸리티
 *
 * <pre>
 * DB의 tag_value(Gemini 친화적 영문 설명)를 받아 Gemini 프롬프트 생성 단계에서 사용하는
 * 텍스트 오버레이 지시사항 문자열을 생성한다.
 * textContent가 null/blank이면 null을 반환하여 텍스트 오버레이 없이 처리한다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-26
 */
public class TextOverlayUtil {

    private TextOverlayUtil() {}

    /**
     * 텍스트 오버레이 정보를 Gemini 프롬프트 지시사항 문자열로 변환한다.
     *
     * <pre>
     * position, size, style 값은 DB tag_value(영문 설명)를 그대로 사용한다.
     * 예: position = "positioned in the top-left corner"
     *     size     = "small text (approximately 3~5% of image height)"
     *     style    = "natural handwriting-style (Script) font"
     * </pre>
     *
     * @param textContent 삽입할 텍스트 내용 (null/blank이면 null 반환)
     * @param position    위치 설명 (DB tag_value, nullable)
     * @param size        크기 설명 (DB tag_value, nullable)
     * @param style       글자체 설명 (DB tag_value, nullable)
     * @param color       텍스트 색상 (자유 입력, nullable)
     * @return Gemini 프롬프트에 포함할 지시사항 문자열, textContent 없으면 null
     */
    public static String buildInstruction(String textContent, String position,
                                          String size, String style, String color) {
        if (textContent == null || textContent.isBlank()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Text Overlay Request:");
        sb.append("\n  - Text: \"").append(textContent.strip()).append("\"");
        if (position != null && !position.isBlank()) {
            sb.append("\n  - Position: ").append(position);
        }
        if (size != null && !size.isBlank()) {
            sb.append("\n  - Size: ").append(size);
        }
        if (style != null && !style.isBlank()) {
            sb.append("\n  - Style: ").append(style);
        }
        if (color != null && !color.isBlank()) {
            sb.append("\n  - Color: ").append(color);
        }
        sb.append("\n  Note: Integrate the text naturally into the scene composition, " +
                  "making it look like an intentional design element that blends with the background.");
        return sb.toString();
    }
}

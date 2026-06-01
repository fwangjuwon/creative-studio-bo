package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

/**
 * 이미지 텍스트 오버레이 지시사항 빌더
 *
 * <pre>
 * Gemini 프롬프트 생성 단계(1단계)에서 user message에 자연어로 통합되도록 출력한다.
 * NanoBanana(서술형)와 MidJourney(키워드)에 맞는 두 가지 포맷을 제공한다.
 * textContent가 null/blank이면 null을 반환한다.
 *
 * position, size, style 값은 DB tag_value(영문 설명)를 그대로 사용한다.
 *   예: position = "positioned in the top-left corner"
 *       size     = "small text (approximately 3~5% of image height)"
 *       style    = "natural handwriting-style Script font with a personal and warm feel"
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-26
 */
public class TextOverlayUtil {

    private TextOverlayUtil() {}

    /**
     * NanoBanana(서술형) 입력에 통합할 자연어 한 문장을 생성한다.
     *
     * <pre>
     * 텍스트는 생성 이미지 안에 포함하되, 피사체(상품/인물) 위에 겹치지 않고
     * 주변 빈 배경·여백 공간에 배치하도록 지시한다.
     *
     * 출력 예:
     *   The image must include the text "SALE" rendered as natural handwriting-style Script font,
     *   positioned in the top-left corner, small text (approximately 3~5% of image height),
     *   in white color, placed within the empty background or negative space around the subject
     *   — the text must NOT overlap, cover, or be superimposed on the product or subject itself.
     * </pre>
     *
     * @param textContent 삽입할 텍스트 내용 (null/blank이면 null 반환)
     * @param position    위치 설명 (DB tag_value, nullable)
     * @param size        크기 설명 (DB tag_value, nullable)
     * @param style       글자체 설명 (DB tag_value, nullable)
     * @param color       텍스트 색상 (자유 입력, nullable)
     * @return Gemini user message에 통합할 한 문장 narrative, textContent 없으면 null
     */
    public static String buildNarrative(String textContent, String position,
                                        String size, String style, String color) {
        if (textContent == null || textContent.isBlank()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("The image must include the text \"").append(textContent.strip()).append("\"");
        if (style != null && !style.isBlank()) {
            sb.append(" rendered as ").append(style);
        }
        if (position != null && !position.isBlank()) {
            sb.append(", ").append(position);
        }
        if (size != null && !size.isBlank()) {
            sb.append(", ").append(size);
        }
        if (color != null && !color.isBlank()) {
            sb.append(", in ").append(color).append(" color");
        }
        sb.append(", placed within the empty background or negative space around the subject"
                + " — the text must NOT overlap, cover, or be superimposed on the product or subject itself.");
        return sb.toString();
    }

    /**
     * MidJourney 키워드형 입력에 통합할 쉼표 키워드 문자열을 생성한다.
     *
     * <pre>
     * 출력 예:
     *   text "SALE", natural handwriting-style Script font, positioned in the top-left corner,
     *   small text (approximately 3~5% of image height), white color,
     *   in background negative space, not overlapping the subject
     * </pre>
     *
     * @return 쉼표로 구분된 키워드 묶음, textContent 없으면 null
     */
    public static String buildKeywords(String textContent, String position,
                                       String size, String style, String color) {
        if (textContent == null || textContent.isBlank()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("text \"").append(textContent.strip()).append("\"");
        if (style != null && !style.isBlank()) {
            sb.append(", ").append(style);
        }
        if (position != null && !position.isBlank()) {
            sb.append(", ").append(position);
        }
        if (size != null && !size.isBlank()) {
            sb.append(", ").append(size);
        }
        if (color != null && !color.isBlank()) {
            sb.append(", ").append(color).append(" color");
        }
        sb.append(", in background negative space, not overlapping the subject");
        return sb.toString();
    }
}

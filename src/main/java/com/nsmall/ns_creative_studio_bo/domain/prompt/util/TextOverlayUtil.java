package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 이미지 텍스트 오버레이 Gemini 지시사항 생성 유틸리티
 *
 * <pre>
 * 메인 텍스트는 항상 홈쇼핑 방송 자막 스타일(굵은 외곽선, 입체감/그림자, 이미지 분위기에 맞는 컬러·질감)로
 * 렌더링하도록 지시한다 (buildBroadcastStyleHint). 줄 배치(한 줄 vs 어절별 스택)와 전체 구도
 * (사진 위 오버레이 vs 포스터처럼 텍스트 전용 구역을 분리한 구성)도 AI가 자유롭게 선택하도록 한다.
 * textContent가 null/blank이면 null을 반환하여 텍스트 오버레이 없이 처리한다.
 *
 * 메인 텍스트 내 "**단어**" 마크다운 강조 문법은 parseEmphasis()로 파싱되어,
 * 기본 자막 스타일보다 한 단계 더 크게 + 단어의 의미·이미지 분위기에 맞는 재질/텍스처 효과
 * (예: 냉감→서리/얼음 결정)를 입혀 강조하라는 지시로 변환된다 (buildEmphasisHint).
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-26
 */
public class TextOverlayUtil {

    private TextOverlayUtil() {}

    /** "**단어**" 강조 마커 패턴 */
    private static final Pattern EMPHASIS_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");

    /**
     * 마크다운 강조(**단어**) 파싱 결과.
     *
     * @param displayText      강조 마커가 제거된 실제 표시 텍스트
     * @param emphasizedWords  강조 마커로 감싸졌던 단어 목록 (순서 유지, 중복 허용)
     */
    public record EmphasisResult(String displayText, List<String> emphasizedWords) {}

    /**
     * 텍스트에서 "**단어**" 마커를 찾아 강조 단어 목록을 추출하고, 마커가 제거된 표시 텍스트를 반환한다.
     * 강조 마커가 없으면 emphasizedWords는 빈 리스트.
     */
    public static EmphasisResult parseEmphasis(String text) {
        Matcher matcher = EMPHASIS_PATTERN.matcher(text);
        List<String> words = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            plain.append(text, lastEnd, matcher.start());
            String word = matcher.group(1);
            plain.append(word);
            words.add(word);
            lastEnd = matcher.end();
        }
        plain.append(text.substring(lastEnd));
        return new EmphasisResult(plain.toString(), words);
    }

    /**
     * 메인 텍스트를 홈쇼핑 방송 자막 스타일(굵은 외곽선, 입체감/그림자, 이미지 분위기와 어울리는 컬러·질감)로
     * 렌더링하라는 지시문을 생성한다. 강조 단어 유무와 무관하게 메인 텍스트에는 항상 적용된다.
     */
    public static String buildBroadcastStyleHint() {
        return " Style: bold broadcast/TV-shopping caption — thick lettering, crisp outline, subtle 3D emboss, "
                + "soft drop shadow, colors/textures matching the image's mood "
                + "(e.g. icy tones for cool, warm tones for hot). "
                + "Layout: freely choose a single flowing line, stacked title-card lines, or a poster-style "
                + "composition with the subject confined to part of the frame and dedicated text zones — "
                + "whichever best fits this text and image, without distorting the subject.";
    }

    /**
     * 강조 단어 목록을 바탕으로 "기본 자막 스타일보다 한 단계 더 크고, 단어의 의미·이미지 분위기에 맞는
     * 재질/텍스처 효과(예: 냉감→서리/얼음 결정, 불맛→화염 효과, 프리미엄→메탈릭 광택)를 입혀 강조"하는
     * 지시문을 생성한다. 강조 단어가 없으면 빈 문자열을 반환한다.
     */
    public static String buildEmphasisHint(List<String> emphasizedWords) {
        if (emphasizedWords.isEmpty()) return "";
        String quoted = emphasizedWords.stream()
                .map(w -> "\"" + w + "\"")
                .collect(Collectors.joining(", "));
        return " Make " + quoted + " the visual focal point — bigger and bolder than the rest, "
                + "with a thematic texture matching its meaning and the image's mood "
                + "(e.g. frost for \"cold\", flame for \"spicy\", metallic sheen for \"premium\") "
                + "rather than a flat color.";
    }

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
            EmphasisResult emphasis = parseEmphasis(textContent.strip());
            String display  = emphasis.displayText().replace("\n", " / ");
            String hint     = emphasis.displayText().contains("\n")
                    ? " Each ' / ' in the text marks a suggested line break, but feel free to adjust the line layout as described below if a different breakdown looks better."
                    : "";
            sb.append("Naturally integrate the text \"").append(display).append("\" into the image,")
              .append(hint)
              .append(" ensuring the exact spelling is rendered correctly and remains highly legible.")
              .append(buildBroadcastStyleHint())
              .append(buildEmphasisHint(emphasis.emphasizedWords()));
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

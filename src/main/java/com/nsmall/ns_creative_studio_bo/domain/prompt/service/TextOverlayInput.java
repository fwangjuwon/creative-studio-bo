package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

/**
 * Gemini 프롬프트 생성 시 텍스트 오버레이 정보 전달용 DTO
 *
 * <pre>
 * ※ 내부 전용 DTO — API 응답에 직접 노출되지 않음.
 *
 * PromptServiceImpl/HumanPromptServiceImpl이 tagId → tag_value를 미리 조회하여
 * GeminiService 호출 시 함께 넘긴다. textContent가 비어 있으면 오버레이를 적용하지 않는다.
 * </pre>
 *
 * @param textContent       삽입할 텍스트 내용 (없으면 오버레이 비활성화)
 * @param textColor         텍스트 색상 자유 입력 (nullable)
 * @param positionTagValue  텍스트 위치 영문 설명 (tag.tag_value, nullable)
 * @param sizeTagValue      텍스트 크기 영문 설명 (tag.tag_value, nullable)
 * @param styleTagValue     글자체 영문 설명 (tag.tag_value, nullable)
 *
 * @author gaeddo
 * @since 2026-05-27
 */
public record TextOverlayInput(
        String textContent,
        String textColor,
        String positionTagValue,
        String sizeTagValue,
        String styleTagValue
) {
    public boolean hasContent() {
        return textContent != null && !textContent.isBlank();
    }
}

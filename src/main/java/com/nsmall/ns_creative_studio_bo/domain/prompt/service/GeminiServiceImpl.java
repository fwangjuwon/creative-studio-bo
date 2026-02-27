package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Gemini API 호출 서비스 구현체
 *
 * <pre>
 * Spring RestClient를 사용하여 Gemini REST API를 호출한다.
 * 호출 실패 시 별도 처리 없이 GlobalExceptionHandler 500 응답으로 위임한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    /**
     * 이미지 생성 프롬프트 작성을 위한 시스템 프롬프트.
     *
     * <pre>
     * 프롬프트 품질은 비즈니스 로직의 일부이므로 환경 설정이 아닌 코드로 관리한다.
     * 변경 시 코드 리뷰를 통해 품질 영향을 검토할 것.
     * Gemini 이미지 생성 가이드(6단계 구조) 기반:
     *   Opening → Setting → Lighting → Camera → Mood &amp; Style → Aspect Ratio
     * </pre>
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert AI image prompt engineer specializing in high-quality commercial
            product photography prompts for AI image generation tools such as Gemini and Midjourney.

            Given a product category and style tags, generate a single vivid English image prompt
            following this exact structure:

            1. OPENING — Shot type + product description:
               "A [shot type, e.g. high-resolution studio-lit / close-up macro / elevated 45-degree]
                product photograph of [detailed product description based on category]"

            2. SETTING — Background and surface:
               "placed on / set against [specific background, e.g. polished marble, weathered oak,
                clean white seamless backdrop, natural linen fabric]"

            3. LIGHTING — Specific setup and purpose:
               "The lighting is [specific setup, e.g. soft three-point softbox / warm golden-hour
                natural light] to [purpose, e.g. highlight texture, eliminate harsh shadows,
                create depth and dimension]"

            4. CAMERA — Lens, angle, and depth of field:
               "Captured with [lens, e.g. 85mm portrait lens / 50mm macro] at [angle],
                [detail, e.g. creating a soft bokeh background / ultra-sharp focus throughout]"

            5. MOOD & STYLE — Weave the style tags into atmosphere and emotional tone

            6. CLOSING — End with aspect ratio: "Square format." or "Landscape format."
               or "Portrait format." depending on context

            Rules:
            - Write in NARRATIVE PARAGRAPH form — do NOT use comma-separated keyword lists
            - Be hyper-specific: describe textures, materials, colors, and fine details
            - Return ONLY the final prompt text — no labels, no explanations, no preamble
            - Write in English only
            - Length: 3 to 5 descriptive sentences
            """;

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    @Override
    public String generatePrompt(String categoryName, List<String> tagNames) {
        String userMessage = buildUserMessage(categoryName, tagNames);
        GeminiRequest requestBody = buildRequest(userMessage);

        String uri = String.format("/models/%s:generateContent?key=%s",
                geminiProperties.getModel(), geminiProperties.getApiKey());

        log.debug("Gemini API 호출 — model: {}, category: {}, tags: {}",
                geminiProperties.getModel(), categoryName, tagNames);

        GeminiResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return extractText(response);
    }

    private String buildUserMessage(String categoryName, List<String> tagNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product Category: ").append(categoryName);
        if (tagNames != null && !tagNames.isEmpty()) {
            sb.append("\nStyle & Mood Tags: ").append(String.join(", ", tagNames));
        }
        sb.append("\nUse case: High-end commercial e-commerce product photography");
        return sb.toString();
    }

    private GeminiRequest buildRequest(String userMessage) {
        GeminiPart systemPart = new GeminiPart(SYSTEM_PROMPT);
        GeminiSystemInstruction systemInstruction = new GeminiSystemInstruction(List.of(systemPart));

        GeminiPart userPart = new GeminiPart(userMessage);
        GeminiContent userContent = new GeminiContent("user", List.of(userPart));

        return new GeminiRequest(systemInstruction, List.of(userContent));
    }

    private String extractText(GeminiResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            log.error("Gemini API 응답이 비어 있습니다 — candidates 없음");
            throw new BizException(PromptErrorCode.GEMINI_EMPTY_RESPONSE,
                    "Gemini API 응답에 candidates가 없습니다.");
        }
        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            log.error("Gemini API 응답 content가 비어 있습니다 — parts 없음");
            throw new BizException(PromptErrorCode.GEMINI_EMPTY_RESPONSE,
                    "Gemini API 응답에 content parts가 없습니다.");
        }
        String text = candidate.content().parts().get(0).text();
        if (text == null || text.isBlank()) {
            log.error("Gemini API 응답 텍스트가 비어 있습니다.");
            throw new BizException(PromptErrorCode.GEMINI_EMPTY_RESPONSE,
                    "Gemini API가 빈 텍스트를 반환했습니다.");
        }
        return text;
    }

    // ================================================================
    // Gemini REST API 요청/응답 내부 DTO
    // ================================================================

    private record GeminiRequest(
            @JsonProperty("system_instruction") GeminiSystemInstruction systemInstruction,
            List<GeminiContent> contents
    ) {}

    private record GeminiSystemInstruction(List<GeminiPart> parts) {}

    private record GeminiContent(String role, List<GeminiPart> parts) {}

    private record GeminiPart(String text) {}

    private record GeminiResponse(List<GeminiCandidate> candidates) {}

    private record GeminiCandidate(GeminiContent content) {}
}

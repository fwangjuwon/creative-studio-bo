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
import java.util.Map;
import java.util.TreeMap;

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
    private static final String GOODS_SYSTEM_PROMPT = """
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

    /**
     * 인물/모델 사진 전용 시스템 프롬프트 (7단계 구조).
     *
     * <pre>
     * 인물 중심 portrait/fashion 사진에 최적화된 구조.
     * </pre>
     */
    private static final String SYSTEM_PROMPT_PERSON = """
            You are an expert AI portrait and fashion photography prompt engineer specializing
            in high-quality human subject photography for AI image generation tools such as
            Gemini and Midjourney.

            Given a subject/category and style tags, generate a single vivid English portrait prompt
            following this exact structure:

            1. SUBJECT — Age aesthetic, gender presentation, pose, and facial expression:
               "A [age aesthetic, e.g. young adult / mid-30s] [gender presentation, e.g. woman / man]
                [pose, e.g. standing with relaxed posture / seated with crossed legs]
                [expression, e.g. with a confident subtle smile / gazing thoughtfully into the distance]"

            2. WARDROBE & STYLING — Clothing details, accessories, hair style and color:
               Describe specific fabrics, fits, colors, and hair characteristics

            3. SETTING — Location and background environment:
               "in / against [specific environment, e.g. a sunlit minimalist studio / an urban
                brick-wall alley / a lush green park at golden hour]"

            4. LIGHTING — Portrait lighting setup and its effect:
               "Lit with [setup, e.g. soft Rembrandt lighting / diffused window light /
                warm rim lighting] to [effect, e.g. sculpt facial features / create a
                dreamy ethereal glow / add depth and separation from the background]"

            5. CAMERA — Lens choice, aperture, and angle:
               "Shot on [lens, e.g. 85mm f/1.4 portrait lens / 50mm f/1.8] at [angle,
                e.g. eye level / slight low angle], [depth of field detail,
                e.g. with a creamy bokeh background / ultra-sharp throughout]"

            6. MOOD & STYLE — Weave the style tags into the overall emotional tone and atmosphere

            7. CLOSING — End with aspect ratio: "Portrait format." or "Square format."
               or "Landscape format." depending on context

            Rules:
            - Write in NARRATIVE PARAGRAPH form — do NOT use comma-separated keyword lists
            - Be hyper-specific: describe skin tone, hair texture, fabric materials, and fine details
            - Return ONLY the final prompt text — no labels, no explanations, no preamble
            - Write in English only
            - Length: 3 to 5 descriptive sentences
            """;

    /**
     * 인물/모델 사진 전용 시스템 프롬프트 — MidJourney 키워드 나열형.
     *
     * <pre>
     * MidJourney는 쉼표로 구분된 짧은 키워드 나열 방식이 최적화되어 있다.
     * 문장형이 아닌 설명 키워드 + 기술 파라미터 형식으로 생성.
     * </pre>
     */
    private static final String SYSTEM_PROMPT_PERSON_MIDJOURNEY = """
            You are an expert AI prompt engineer specializing in MidJourney portrait photography prompts.
            MidJourney works best with comma-separated keyword descriptors, NOT full sentences.

            Given a subject/category and style tags, generate a single concise English MidJourney prompt
            as a flat comma-separated keyword list following this order:

            1. SUBJECT — age, gender, and key physical traits (e.g. "young Korean woman, early 20s")
            2. POSE & EXPRESSION — (e.g. "standing relaxed, confident smile")
            3. WARDROBE & HAIR — key clothing and hair descriptors (e.g. "white linen blouse, wavy black hair")
            4. SETTING — background keywords (e.g. "sunlit studio, clean white seamless backdrop")
            5. LIGHTING — lighting style keywords (e.g. "soft diffused window light, warm rim light")
            6. CAMERA — lens and shot type (e.g. "85mm f/1.4, shallow depth of field, medium shot")
            7. STYLE & MOOD — atmosphere and quality keywords from style tags
            8. TECHNICAL — quality boosters: photorealistic, ultra-detailed, professional fashion photography
            9. ASPECT RATIO — end with "--ar 2:3" (portrait) or "--ar 1:1" (square) or "--ar 3:2" (landscape)

            Rules:
            - Output ONLY comma-separated keyword phrases — absolutely NO full sentences
            - Keep each element concise: 1–5 words per descriptor
            - Return ONLY the final prompt — no labels, no explanations, no preamble
            - Write in English only
            - Example output: "young Korean woman, early 20s, standing relaxed, confident smile, white linen blouse,\s
              wavy black hair, sunlit minimalist studio, soft diffused window light, warm golden tones,\s
              85mm f/1.4, medium shot, bokeh background, photorealistic, ultra-detailed, --ar 2:3"
            """;

    /**
     * Step별 레이블 (user message 포맷용)
     */
    private static final Map<Integer, String> STEP_LABELS = new TreeMap<>(Map.of(
            1, "Subject",
            2, "Mood & Style",
            3, "Realism",
            4, "Lighting & Setting",
            5, "Camera"
    ));

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    @Override
    public String generateHumanPrompt(Map<Integer, List<String>> stepOptionValues,
                                      String purpose, String additionalNotes, String promptStyle) {
        StringBuilder sb = new StringBuilder();

        // step 번호 오름차순으로 user message 구성
        STEP_LABELS.forEach((stepNumber, label) -> {
            List<String> values = stepOptionValues.get(stepNumber);
            if (values != null && !values.isEmpty()) {
                sb.append("Step ").append(stepNumber)
                  .append(" (").append(label).append("): ")
                  .append(String.join(", ", values))
                  .append("\n");
            }
        });

        if (purpose != null && !purpose.isBlank()) {
            sb.append("Purpose: ").append(purpose).append("\n");
        }
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            sb.append("Additional notes: ").append(additionalNotes).append("\n");
        }
        sb.append("Use case: High-end commercial portrait photography");

        String userMessage = sb.toString();

        // promptStyle에 따라 시스템 프롬프트 선택
        boolean isMidJourney = "MID_JOURNEY".equalsIgnoreCase(promptStyle);
        String systemPromptText = isMidJourney ? SYSTEM_PROMPT_PERSON_MIDJOURNEY : SYSTEM_PROMPT_PERSON;

        GeminiPart systemPart = new GeminiPart(systemPromptText);
        GeminiSystemInstruction systemInstruction = new GeminiSystemInstruction(List.of(systemPart));
        GeminiPart userPart = new GeminiPart(userMessage);
        GeminiContent userContent = new GeminiContent("user", List.of(userPart));
        GeminiRequest requestBody = new GeminiRequest(systemInstruction, List.of(userContent));

        String uri = String.format("/models/%s:generateContent?key=%s",
                geminiProperties.getModel(), geminiProperties.getApiKey());

        log.debug("Gemini 인물 사진 API 호출 — model: {}, style: {}, steps: {}",
                geminiProperties.getModel(), promptStyle, stepOptionValues.keySet());

        GeminiResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return extractText(response);
    }

    @Override
    public String generateGoodsPrompt(String goodsCategoryName, List<String> tagNames,
                                      String purpose, String additionalNotes) {
        String userMessage = buildGoodsUserMessage(goodsCategoryName, tagNames, purpose, additionalNotes);
        GeminiRequest requestBody = buildRequest(userMessage);

        String uri = String.format("/models/%s:generateContent?key=%s",
                geminiProperties.getModel(), geminiProperties.getApiKey());

        log.debug("Gemini API 호출 — model: {}, goodsCategory: {}, tags: {}",
                geminiProperties.getModel(), goodsCategoryName, tagNames);

        GeminiResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return extractText(response);
    }

    private String buildGoodsUserMessage(String goodsCategoryName, List<String> tagNames,
                                         String purpose, String additionalNotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product Category: ").append(goodsCategoryName);
        if (tagNames != null && !tagNames.isEmpty()) {
            sb.append("\nStyle & Mood Tags: ").append(String.join(", ", tagNames));
        }
        if (purpose != null && !purpose.isBlank()) {
            sb.append("\nPurpose: ").append(purpose);
        }
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            sb.append("\nAdditional notes: ").append(additionalNotes);
        }
        sb.append("\nUse case: High-end commercial e-commerce product photography");
        return sb.toString();
    }

    private GeminiRequest buildRequest(String userMessage) {
        GeminiPart systemPart = new GeminiPart(GOODS_SYSTEM_PROMPT);
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

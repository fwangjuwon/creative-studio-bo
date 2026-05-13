package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
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
 *
 * 개선 포인트:
 * 1. 시스템 프롬프트에 재질(Texture), 반사(Reflection), 인물 디테일(Catchlight) 가이드 추가
 * 2. Few-shot 예시를 포함하여 일관된 출력 품질 확보
 * 3. Midjourney 파라미터 결합 로직 최적화
 * </pre>
 *
 * @author gaeddo
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    /**
     * 상품 사진 전용 시스템 프롬프트.
     *
     * <pre>
     * 프롬프트 품질은 비즈니스 로직의 일부이므로 환경 설정이 아닌 코드로 관리한다.
     * 변경 시 코드 리뷰를 통해 품질 영향을 검토할 것.
     * Gemini 이미지 생성 가이드(6단계 구조) 기반:
     *   Opening → Setting → Lighting → Camera → Style → Aspect Ratio
     * </pre>
     */
    private static final String GOODS_SYSTEM_PROMPT = """
        You are a professional commercial photographer and prompt engineer.
        Your goal is to create a vivid, 3-5 sentence narrative English prompt for high-end product photography.

        STRUCTURE:
        1. OPENING: Shot type + Detailed product description (Focus on materials like 'brushed metal', 'matte ceramic', 'translucent glass').
        2. SETTING: Background, surface, and complementary props (e.g., organic elements, minimalist shapes).
        3. LIGHTING: Specific setup (Softbox, rim lighting) and its physical effect (e.g., 'creating sharp contact shadows', 'defining edges').
        4. CAMERA: Lens (85mm, 100mm macro), angle, and depth of field.
           - Negative Space: Leave intentional empty areas in the frame for advertising copy placement.
           - Leading Lines: Design background elements (edges, surfaces, shadows) that guide the eye toward the product.
           - Color Theory: Apply "complementary color palette" for bold contrast or "monochromatic elegance" for refined cohesion.
        5. STYLE: Mood, color palette, and premium atmosphere.

        QUALITY RULES:
        - Describe how light interacts with the surface (reflections, subsurface scattering).
        - No CGI or plastic looks; focus on hyper-realistic commercial quality.
        - Refraction & Caustics: Pay extreme attention to light refraction through transparent materials and ensure realistic caustics are cast on the surface.
        - Subsurface Scattering (SSS): For translucent materials (milk, soap, wax), emphasize the soft inner glow of light scattering beneath the surface.
        - Micro-surface details: Capture ultra-fine surface details such as metal micro-scratches, leather pores, or fabric weave texture.
        - Utilize 'Ray-tracing' logic to render physically accurate reflections and shadows, ensuring the product feels grounded on its surface.

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no text artifacts, no logo
        - no overexposure, no blown highlights

        COMPOSITION:
        - Use professional composition (rule of thirds, centered composition)
        - Ensure clear subject focus with strong isolation

        - Return ONLY the final prompt text.

        EXAMPLE:
        "A high-resolution 45-degree product photograph of a premium matte-finish skincare bottle. It is placed on a damp dark slate surface with subtle water ripples. The lighting is a cool-toned side softbox that highlights the bottle's elegant silhouette and creates soft contact shadows. Captured with a 100mm macro lens for a clean professional focus."
        """;

    /**
     * 상품 사진 전용 시스템 프롬프트 — MidJourney 키워드 나열형.
     *
     * <pre>
     * MidJourney는 쉼표로 구분된 짧은 키워드 나열 방식이 최적화되어 있다.
     * 문장형이 아닌 설명 키워드 + 기술 파라미터 형식으로 생성.
     * </pre>
     */
    private static final String GOODS_SYSTEM_PROMPT_MIDJOURNEY = """
        You are an expert Midjourney prompt engineer for commercial products. 
        Output ONLY a comma-separated keyword list. No sentences.

        STRUCTURE:
        1. Product (Material, Color)
        2. Shot Type (Macro, Eye-level, Top-down)
        3. Setting (Surface, Background, Props)
        4. Lighting (Studio lighting, Volumetric, God rays)
        5. Camera (Lens, angle, depth of field, Negative Space for ad copy, Leading Lines, Color Theory: complementary or monochromatic)
        6. Quality Boosters (8k, Photorealistic, Ray tracing, Highly detailed)
        7. Aspect Ratio (End with --ar X:X)

        CRITICAL: Focus on textures like 'brushed aluminum', 'fine-grain leather', or 'crystal clear glass'.
        - Refraction & Caustics: light refraction through glass, realistic caustics on surface
        - Subsurface Scattering (SSS): inner glow for translucent materials (milk, soap, wax)
        - Micro-surface details: metal micro-scratches, leather pores, fabric weave texture

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no text artifacts, no logo
        - no overexposure, no blown highlights

        COMPOSITION:
        - Use professional composition (rule of thirds, centered composition)
        - Ensure clear subject focus with strong isolation

        EXAMPLE:
        Luxury wristwatch, brushed gold and sapphire glass, close-up macro, dark marble table, dramatic rim lighting, soft bokeh, 8k, photorealistic, premium commercial photography --ar 1:1
        """;

    /**
     * 인물/모델 사진 전용 시스템 프롬프트 (문장형).
     *
     * <pre>
     * 인물 중심 portrait/fashion 사진에 최적화된 구조.
     * 인체 리얼리즘(피부, 눈, 손) 및 uncanny valley 방지 규칙 포함.
     * </pre>
     */
    private static final String SYSTEM_PROMPT_PERSON = """
        You are a high-end fashion and portrait photographer. 
        Generate a 3-5 sentence narrative English prompt focusing on human realism.

        STRUCTURE:
        1. SUBJECT — exact number of people, age, gender, and key physical traits:
           CRITICAL: Use the EXACT person count from the provided subject value.
           Do NOT add or remove people under any circumstances.
        2. SKIN & TEXTURE: Emphasize 'natural skin pores', 'fine hair details', and 'subtle catchlights in the eyes'.
        3. WARDROBE: Specific fabric materials (Silk, wool, linen) and styling.
        4. SETTING: Environmental context and atmosphere.
        5. LIGHTING & CAMERA: Professional portrait lighting (Rembrandt, Butterfly) and lens (35mm for environmental, 85mm for headshots).
           - Negative Space: Leave intentional empty areas around the subject for editorial use.
           - Leading Lines: Use environmental elements (walls, paths, light beams) to draw the viewer's eye to the subject.
           - Color Theory: Apply "complementary color palette" for bold contrast or "monochromatic elegance" for refined cohesion.

        QUALITY RULES:
        - Avoid 'uncanny valley' or 'plastic skin'. Ensure anatomical correctness (especially hands).
        - Subsurface Scattering (SSS): Emphasize the soft translucency of skin where light scatters beneath the surface (ears, nose tip, cheeks).
        - Micro-surface details: Capture fine skin pores, hair strands, and fabric weave texture at ultra-high detail.

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no text artifacts, no logo
        - no overexposure, no blown highlights

        COMPOSITION:
        - Use professional composition (rule of thirds, centered composition)
        - Ensure clear subject focus with strong isolation

        HAND & ANATOMY:
        - hands must be natural with correct finger count
        - no extra limbs, no deformed fingers

        - Return ONLY the final prompt text.

        EXAMPLE:
        "A professional portrait of a confident middle-aged woman in a silk navy blazer, posing naturally in a bright minimalist office. Her skin shows natural textures and subtle pores under soft, diffused window lighting that creates gentle catchlights in her eyes. Captured with an 85mm lens at f/1.8, creating a creamy bokeh background."
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
        You are an expert Midjourney prompt engineer for fashion portraits. 
        Output ONLY a comma-separated keyword list. No sentences.

        REQUIRED TAGS:
        Photorealistic, highly detailed skin pores, natural skin texture, subsurface scattering, micro-surface details, masterpiece, 8k, anatomical correctness, natural eyes.

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no text artifacts, no logo
        - no overexposure, no blown highlights

        COMPOSITION:
        - Use professional composition (rule of thirds, centered composition)
        - Ensure clear subject focus with strong isolation

        HAND & ANATOMY:
        - hands must be natural with correct finger count
        - no extra limbs, no deformed fingers

        STRUCTURE:
        1. Subject — CRITICAL: Use the EXACT person count from the provided subject value. Do NOT add or remove people.
           (Pose, Expression) 2. Wardrobe 3. Setting 4. Lighting (Catchlights, Cinematographic) 5. Camera (Depth of field, Negative Space for editorial use, Leading Lines, Color Theory: complementary or monochromatic) 6. Aspect Ratio (--ar X:X)

        EXAMPLE:
        Young male model, neutral expression, linen shirt, sunny Mediterranean balcony, golden hour lighting, catchlights, shot on 35mm lens, high-end fashion photography, ultra-realistic --ar 2:3
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
                                      String purpose, String additionalNotes, PromptStyle promptStyle,
                                      String imageAspectRatio) {
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
        boolean isMidJourney = PromptStyle.MID_JOURNEY == promptStyle;
        String systemPromptText = isMidJourney ? SYSTEM_PROMPT_PERSON_MIDJOURNEY : SYSTEM_PROMPT_PERSON;

        GeminiPart systemPart = new GeminiPart(systemPromptText);
        GeminiSystemInstruction systemInstruction = new GeminiSystemInstruction(List.of(systemPart));
        GeminiPart userPart = new GeminiPart(userMessage);
        GeminiContent userContent = new GeminiContent("user", List.of(userPart));
        GeminiRequest requestBody = new GeminiRequest(systemInstruction, List.of(userContent));

        String uri = String.format("/models/%s:generateContent", geminiProperties.getModel());

        log.debug("Gemini 인물 사진 API 호출 — model: {}, style: {}, steps: {}",
                geminiProperties.getModel(), promptStyle, stepOptionValues.keySet());

        GeminiResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        String promptText = extractText(response);

        String formatSuffix = toFormatSuffix(imageAspectRatio, isMidJourney);
        if (formatSuffix != null) {
            if (isMidJourney) {
                // Gemini가 자체 생성한 --ar X:X 제거 후 사용자 선택 비율로 교체
                promptText = promptText.strip()
                        .replaceAll(",?\\s*--ar\\s+\\d+:\\d+", "")
                        .strip() + ", " + formatSuffix;
            } else {
                promptText = promptText.strip() + " " + formatSuffix;
            }
        }

        return promptText;
    }

    @Override
    public String generateGoodsPrompt(String goodsCategoryName, List<String> tagNames,
                                      String imageAspectRatio, String purpose, String additionalNotes,
                                      PromptStyle promptStyle) {
        String userMessage = buildGoodsUserMessage(goodsCategoryName, tagNames, purpose, additionalNotes);

        boolean isMidJourney = PromptStyle.MID_JOURNEY == promptStyle;
        String systemPromptText = isMidJourney ? GOODS_SYSTEM_PROMPT_MIDJOURNEY : GOODS_SYSTEM_PROMPT;
        GeminiRequest requestBody = buildRequest(userMessage, systemPromptText);

        String uri = String.format("/models/%s:generateContent", geminiProperties.getModel());

        log.debug("Gemini API 호출 — model: {}, goodsCategory: {}, tags: {}, aspectRatio: {}, style: {}",
                geminiProperties.getModel(), goodsCategoryName, tagNames, imageAspectRatio, promptStyle);

        GeminiResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        String promptText = extractText(response);

        // 이미지 비율이 선택된 경우 format suffix를 Java에서 직접 추가
        // Gemini가 자체 붙인 format suffix(또는 --ar)를 먼저 제거 후 교체
        String formatSuffix = toFormatSuffix(imageAspectRatio, isMidJourney);
        if (formatSuffix != null) {
            if (isMidJourney) {
                // Gemini가 자체 생성한 --ar X:X 제거 후 사용자 선택 비율로 교체
                promptText = promptText.strip()
                        .replaceAll(",?\\s*--ar\\s+\\d+:\\d+", "")
                        .strip() + ", " + formatSuffix;
            } else {
                promptText = promptText.strip() + " " + formatSuffix;
            }
        }

        return promptText;
    }

    /**
     * imageAspectRatio와 promptStyle에 따라 적절한 format suffix를 반환한다.
     *
     * <pre>
     * - MidJourney: "--ar W:H" 형식으로 직접 전달
     * - NanoBanana: "Aspect ratio W:H." 형식으로 동적 생성 (하드코딩 없음)
     *   → DB 태그명이 무엇이든 자동 반영, Admin이 새 비율 추가 시 코드 수정 불필요
     * </pre>
     *
     * @param imageAspectRatio 사용자가 선택한 이미지 비율 태그명
     * @param isMidJourney     MidJourney 스타일 여부
     * @return format suffix 문자열, null이면 미선택
     */
    private String toFormatSuffix(String imageAspectRatio, boolean isMidJourney) {
        if (imageAspectRatio == null) return null;
        if (isMidJourney) {
            return "--ar " + imageAspectRatio;
        }
        return "Aspect ratio " + imageAspectRatio + ".";
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

    private GeminiRequest buildRequest(String userMessage, String systemPromptText) {
        GeminiPart systemPart = new GeminiPart(systemPromptText);
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

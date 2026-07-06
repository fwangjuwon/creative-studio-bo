package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.AspectRatioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;


/**
 * Gemini API 호출 서비스 구현체
 *
 * <pre>
 * Spring RestClient를 사용하여 Gemini REST API를 호출한다.
 * API 호출 실패 시 {@link PromptErrorCode#GEMINI_API_ERROR}로 변환하여 던진다.
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
        Your goal is to create a vivid, 5-7 sentence narrative English prompt for high-end product photography.

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
        - no unintentional text artifacts, no watermark, no logo
        - no overexposure, no blown highlights

        COMPOSITION:
        - Use professional composition (rule of thirds, centered composition)
        - Ensure clear subject focus with strong isolation

        - Return ONLY the final prompt text.

        EXAMPLE:
        "A high-resolution 45-degree product photograph of a premium matte-finish skincare bottle. It is placed on a damp dark slate surface with subtle water ripples. The lighting is a cool-toned side softbox that highlights the bottle's elegant silhouette and creates soft contact shadows. The rim of the bottle catches a subtle warm highlight from a secondary fill light, emphasizing its refined contour. Captured with a 100mm macro lens at f/4 for a clean professional focus with a soft bokeh background. The overall palette is cool grey and slate, evoking a sense of clinical purity and premium skincare luxury."
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
        - no unintentional text artifacts, no watermark, no logo
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
     * 다인원 입력(Person 1 / Person 2 ...)을 이해하고 각 인물의 특성을 반영한다.
     * </pre>
     */
    private static final String SYSTEM_PROMPT_PERSON = """
        You are a high-end fashion and portrait photographer.
        Generate a narrative English prompt focusing on human realism.

        INPUT FORMAT:
        - Single person: one block of step options
        - Multiple persons (1~5): "Person 1:", "Person 2:", ... blocks — describe each individually
        - Group (6+): "Group (6+ people):" block — describe as a cohesive group scene

        STRUCTURE (apply per person or for the group):
        1. SUBJECT — describe each person's count, age, gender, and key physical traits individually.
           CRITICAL: Reflect the EXACT characteristics provided for each person. Do NOT mix up traits between persons.
        2. SKIN & TEXTURE: Emphasize 'natural skin pores', 'fine hair details', and 'subtle catchlights in the eyes'.
        3. WARDROBE: Specific fabric materials (Silk, wool, linen) and styling.
        4. SETTING: Environmental context and atmosphere that fits all subjects.
        5. LIGHTING & CAMERA: Professional portrait lighting (Rembrandt, Butterfly) and lens (35mm for environmental, 85mm for headshots).
           - Negative Space: Leave intentional empty areas around the subject for editorial use.
           - Leading Lines: Use environmental elements to draw the viewer's eye to the subject.
           - Color Theory: Apply "complementary color palette" for bold contrast or "monochromatic elegance" for refined cohesion.

        QUALITY RULES:
        - Avoid 'uncanny valley' or 'plastic skin'. Ensure anatomical correctness (especially hands).
        - Subsurface Scattering (SSS): Emphasize the soft translucency of skin where light scatters beneath the surface.
        - Micro-surface details: Capture fine skin pores, hair strands, and fabric weave texture at ultra-high detail.

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no unintentional text artifacts, no logo
        - no overexposure, no blown highlights

        HAND & ANATOMY:
        - hands must be natural with correct finger count
        - no extra limbs, no deformed fingers

        - Return ONLY the final prompt text.

        EXAMPLE (2 persons):
        "A professional portrait of two models: a confident middle-aged woman in a silk navy blazer and a young man in a tailored grey suit, both posing naturally in a bright minimalist office. Their skin shows natural textures and subtle pores under soft diffused window lighting that creates gentle catchlights in their eyes. Captured with a 35mm lens at f/2.8."
        """;

    /**
     * 인물/모델 사진 전용 시스템 프롬프트 — MidJourney 키워드 나열형.
     *
     * <pre>
     * MidJourney는 쉼표로 구분된 짧은 키워드 나열 방식이 최적화되어 있다.
     * 다인원 입력을 이해하고 각 인물의 특성을 반영한다.
     * </pre>
     */
    private static final String SYSTEM_PROMPT_PERSON_MIDJOURNEY = """
        You are an expert Midjourney prompt engineer for fashion portraits.
        Output ONLY a comma-separated keyword list. No sentences.

        INPUT FORMAT:
        - Multiple persons (Person 1 / Person 2 ...): reflect each person's traits individually
        - Group (6+): describe as a cohesive group scene

        REQUIRED TAGS:
        Photorealistic, highly detailed skin pores, natural skin texture, subsurface scattering, micro-surface details, masterpiece, 8k, anatomical correctness, natural eyes.

        NEGATIVE:
        - no distortion, no warped geometry, no unrealistic proportions
        - no watermark, no unintentional text artifacts, no logo
        - no overexposure, no blown highlights

        HAND & ANATOMY:
        - hands must be natural with correct finger count
        - no extra limbs, no deformed fingers

        STRUCTURE:
        1. Subject (each person's traits individually, or group description)
        2. Wardrobe 3. Setting 4. Lighting (Catchlights, Cinematographic)
        5. Camera (Depth of field, Negative Space, Leading Lines, Color Theory)
        6. Aspect Ratio (--ar X:X)

        EXAMPLE (2 persons):
        Female model 30s silk blouse, male model 20s linen shirt, together in sunny Mediterranean balcony, golden hour lighting, catchlights, 35mm lens, high-end fashion photography, ultra-realistic --ar 16:9
        """;

    // STEP_NAME_TO_LABEL 제거 — step 이름은 DB에서 관리하므로 하드코딩 없이 그대로 사용
    // Admin에서 단계 추가/수정/삭제 시 코드 변경 없이 자동 반영됨

    /** 인물 프롬프트 생성 시 최대 출력 토큰 수 — 다인원 프롬프트 길이 대응 */
    private static final int HUMAN_PROMPT_MAX_OUTPUT_TOKENS = 4096;

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    @Override
    public String generateHumanPrompt(List<Map<String, Map<String, List<String>>>> personsStepValues,
                                      Map<String, Map<String, List<String>>> commonStepValues,
                                      boolean isGroup,
                                      String relationship, String additionalNotes, PromptStyle promptStyle,
                                      String imageAspectRatio) {
        StringBuilder sb = new StringBuilder();

        // 공통 옵션 (조명/배경, 촬영기술) — 항상 먼저 기술
        if (commonStepValues != null && !commonStepValues.isEmpty()) {
            sb.append("Common Settings (apply to all):\n");
            appendStepValues(sb, commonStepValues);
            sb.append("\n");
        }

        // 인물별 옵션
        if (isGroup) {
            sb.append("Group (6+ people) - Subject Options:\n");
            appendStepValues(sb, personsStepValues.get(0));
        } else if (personsStepValues.size() == 1) {
            sb.append("Subject Options:\n");
            appendStepValues(sb, personsStepValues.get(0));
        } else {
            sb.append("Total persons: ").append(personsStepValues.size()).append("\n\n");
            for (int i = 0; i < personsStepValues.size(); i++) {
                sb.append("Person ").append(i + 1).append(":\n");
                appendStepValues(sb, personsStepValues.get(i));
                sb.append("\n");
            }
        }

        if (relationship != null && !relationship.isBlank()) {
            sb.append("Relationship between persons: ").append(relationship).append("\n");
        }
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            sb.append("Additional notes: ").append(additionalNotes).append("\n");
        }
        sb.append("Use case: High-end commercial portrait photography");

        String userMessage = sb.toString();

        boolean isMidJourney = PromptStyle.MID_JOURNEY == promptStyle;
        String systemPromptText = isMidJourney ? SYSTEM_PROMPT_PERSON_MIDJOURNEY : SYSTEM_PROMPT_PERSON;

        GeminiPart systemPart = new GeminiPart(systemPromptText);
        GeminiSystemInstruction systemInstruction = new GeminiSystemInstruction(List.of(systemPart));
        GeminiPart userPart = new GeminiPart(userMessage);
        GeminiContent userContent = new GeminiContent("user", List.of(userPart));
        GeminiGenerationConfig generationConfig = new GeminiGenerationConfig(HUMAN_PROMPT_MAX_OUTPUT_TOKENS);
        GeminiRequest requestBody = new GeminiRequest(systemInstruction, List.of(userContent), generationConfig);

        String uri = String.format("/models/%s:generateContent", geminiProperties.getModel());

        log.debug("Gemini 인물 사진 API 호출 — model: {}, style: {}, personCount: {}, isGroup: {}",
                geminiProperties.getModel(), promptStyle, personsStepValues.size(), isGroup);

        GeminiResponse response;
        try {
            response = geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Gemini API 오류 응답 (인물 프롬프트) — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(resolvePromptErrorCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.error("Gemini API 호출 실패 (인물 프롬프트, 네트워크/타임아웃)", e);
            throw new BizException(PromptErrorCode.GEMINI_API_ERROR, e);
        }

        String promptText = extractText(response);

        String formatSuffix = toFormatSuffix(imageAspectRatio, isMidJourney);
        if (formatSuffix != null) {
            if (isMidJourney) {
                promptText = promptText.strip()
                        .replaceAll(",?\\s*--ar\\s+\\d+:\\d+", "")
                        .strip() + ", " + formatSuffix;
            } else if (AspectRatioUtil.isWideRatio(imageAspectRatio)) {
                promptText = formatSuffix + " " + promptText.strip();
            } else {
                promptText = promptText.strip() + " " + formatSuffix;
            }
        }

        return promptText;
    }

    /**
     * stepName → (attrName → promptValues) 구조를 Gemini 프롬프트 텍스트로 직렬화한다.
     *
     * <pre>
     * 출력 예시:
     *   핵심 피사체 정의:
     *     - 연령대: Child
     *     - 성별: Man
     *   조명/배경:
     *     - 조명 유형: Soft natural window light
     * </pre>
     *
     * attrName별로 개별 라인으로 출력하여 Gemini가 복수 속성을 여러 인물로 오해하는 버그를 방지한다.
     */
    private void appendStepValues(StringBuilder sb, Map<String, Map<String, List<String>>> stepOptionValues) {
        stepOptionValues.forEach((stepName, attrMap) -> {
            if (attrMap == null || attrMap.isEmpty()) return;
            sb.append("  ").append(stepName).append(":\n");
            attrMap.forEach((attrName, values) -> {
                if (values != null && !values.isEmpty()) {
                    sb.append("    - ").append(attrName).append(": ")
                      .append(String.join(", ", values))
                      .append("\n");
                }
            });
        });
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

        GeminiResponse response;
        try {
            response = geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Gemini API 오류 응답 (상품 프롬프트) — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(resolvePromptErrorCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.error("Gemini API 호출 실패 (상품 프롬프트, 네트워크/타임아웃)", e);
            throw new BizException(PromptErrorCode.GEMINI_API_ERROR, e);
        }

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
            } else if (AspectRatioUtil.isWideRatio(imageAspectRatio)) {
                promptText = formatSuffix + " " + promptText.strip();
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
     * - 가로:세로 비율이 4:1 이상인 경우(미디어월 등) 타일링 방지 지시 추가
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
        if (AspectRatioUtil.isWideRatio(imageAspectRatio)) {
            return "Aspect ratio " + imageAspectRatio + "."
                    + " CRITICAL: This is ONE single continuous ultra-wide photograph."
                    + " NOT a triptych. NOT a diptych. NOT a collage. NOT tiles. NOT a repeated pattern."
                    + " The subject appears exactly once. Do not repeat any element, scene, or composition.";
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

        return new GeminiRequest(systemInstruction, List.of(userContent), null);
    }

    private PromptErrorCode resolvePromptErrorCode(int statusCode) {
        return switch (statusCode) {
            case 429 -> PromptErrorCode.GEMINI_RATE_LIMITED;
            case 403 -> PromptErrorCode.GEMINI_AUTH_FAILED;
            case 500, 503, 504 -> PromptErrorCode.GEMINI_UNAVAILABLE;
            default -> PromptErrorCode.GEMINI_API_ERROR;
        };
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
            List<GeminiContent> contents,
            @JsonProperty("generation_config") GeminiGenerationConfig generationConfig
    ) {}

    private record GeminiGenerationConfig(
            @JsonProperty("max_output_tokens") int maxOutputTokens
    ) {}

    private record GeminiSystemInstruction(List<GeminiPart> parts) {}

    private record GeminiContent(String role, List<GeminiPart> parts) {}

    private record GeminiPart(String text) {}

    private record GeminiResponse(List<GeminiCandidate> candidates) {}

    private record GeminiCandidate(GeminiContent content) {}
}

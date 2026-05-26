package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

/**
 * NanoBanana 이미지 생성 서비스 구현체
 *
 * <pre>
 * Gemini 이미지 생성 모델(gemini-3-pro-image-preview)을 사용하여 이미지를 생성한다.
 * 기존 geminiRestClient Bean을 재사용하며, 모델명만 image-model로 변경한다.
 *
 * 처리 흐름:
 *   1. 프롬프트 텍스트로 Gemini 이미지 생성 API 호출
 *   2. 응답의 inlineData(base64 + mimeType) 추출
 *   3. UUID 파일명으로 storagePath에 저장
 *   4. 정적 리소스 접근 URL 반환 (/images/{uuid}.{ext})
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NanoBananaServiceImpl implements ImageGenerationService {

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    @Value("${image.storage-path}")
    private String storagePath;

    private String generateImage(String prompt) {
        log.debug("NanoBanana 이미지 생성 호출 — model: {}", geminiProperties.getImageModel());

        NbRequest requestBody = new NbRequest(
                List.of(new NbContent("user", List.of(new NbPart(prompt)))),
                new NbGenerationConfig(List.of("IMAGE"))
        );

        return callAndSave(requestBody);
    }

    @Override
    public String generateImageMime(String prompt, byte[] imageBytes, String mimeType) {
        log.debug("NanoBanana 이미지 생성 호출 (참조 이미지 포함) — model: {}", geminiProperties.getImageModel());

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        NbRequest requestBody = new NbRequest(
                List.of(new NbContent("user", List.of(
                        new NbPart(prompt),
                        new NbPart(null, new NbInlineData(mimeType, base64Image))
                ))),
                new NbGenerationConfig(List.of("IMAGE"))
        );

        return callAndSave(requestBody);
    }

    /**
     * Gemini 이미지 생성 API를 호출하고 응답에서 이미지를 추출·저장한 뒤 URL을 반환한다.
     */
    private String callAndSave(NbRequest requestBody) {
        String uri = String.format("/models/%s:generateContent", geminiProperties.getImageModel());

        NbResponse response;
        try {
            response = geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(NbResponse.class);
        } catch (RestClientResponseException e) {
            log.error("NanoBanana API 오류 응답 — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(resolveImageErrorCode(e.getStatusCode().value()));
        } catch (RestClientException e) {
            log.error("NanoBanana API 호출 실패 (네트워크/타임아웃)", e);
            throw new BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED);
        }

        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            log.error("NanoBanana 응답이 비어 있습니다 — candidates 없음");
            throw new BizException(ImageGenerationErrorCode.EMPTY_IMAGE_RESPONSE);
        }

        NbContent content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            log.error("NanoBanana 응답 content가 비어 있습니다");
            throw new BizException(ImageGenerationErrorCode.EMPTY_IMAGE_RESPONSE);
        }

        NbInlineData inlineData = content.parts().get(0).inlineData();
        if (inlineData == null || inlineData.data() == null || inlineData.data().isBlank()) {
            log.error("NanoBanana 응답에 inlineData가 없습니다");
            throw new BizException(ImageGenerationErrorCode.EMPTY_IMAGE_RESPONSE);
        }

        String imageUrl = saveImage(inlineData.data(), inlineData.mimeType());
        log.debug("NanoBanana 이미지 저장 완료 — url: {}", imageUrl);

        return imageUrl;
    }

    @Override
    public List<String> generateImages(String prompt, int count) {
        log.debug("NanoBanana 이미지 {}개 병렬 생성 시작", count);

        // count개 CompletableFuture 생성 후 병렬 실행
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> generateImage(prompt)))
                .toList();

        try {
            List<String> imageUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            log.info("NanoBanana 이미지 {}개 병렬 생성 완료 — urls: {}", count, imageUrls);
            return imageUrls;
        } catch (CompletionException e) {
            // CompletableFuture 내부에서 발생한 BizException을 unwrap하여 재throw
            // → GlobalExceptionHandler의 BizException 핸들러가 올바르게 처리
            if (e.getCause() instanceof BizException bizEx) {
                throw bizEx;
            }
            throw e;
        }
    }

    private static final String FUSION_SYSTEM_PROMPT = """
            [Role]
            You are a high-performance visual synthesis engine (Nano Banana 2) specialized in combining two reference images to create seamless 'interaction' scenes at a professional Commercial Photography level.

            [Core Mission]
            Your primary objective is to output a natural composite image that preserves 100% of the visual identity of both the Person (Reference_1) and the Product (Reference_2), strictly adhering to the laws of physics.

            [Prompt Reconstruction & Interaction Logic]
            Analyze the user's instructions and reconstruct them into a sophisticated internal prompt based on the following rules:

            1. Identity & Branding Preservation
               - Reference_Image_1 (Person): CRITICAL — Treat this image as a strict biometric identity reference. The following must match 100%: exact eye shape and color, nose shape, mouth and lip shape, jawline, skin tone, skin texture, unique facial features (moles, wrinkles, etc.), hairstyle, and hair color. Do NOT generate a similar-looking person. Reproduce the EXACT individual from Reference_Image_1.
               - Reference_Image_2 (Product): Never damage or alter the package design, logo, or brand colors. For brand integrity, ensure the logo is always sharp and oriented toward the camera.

            2. Action-Specific Physics & Interaction
               - 'Holding': Describe the anatomical detail of finger joints firmly grasping the product's edges and thickness. Reflect subtle muscle tension in the hand according to the product's weight.
               - 'Eating/Tasting': Position the product near the mouth and synthesize vivid expressions (e.g., a bright smile, appetizing gaze). Express subtle cheek contours if food is being held in the mouth.
               - 'Pointing/Presenting': Have the person support the product with one hand while pointing at the logo or key features with the index finger. Connect the gaze naturally between the product and the camera.
               - 'Hugging/Embracing': Bring the product close to the chest to show attachment. Render realistic fabric wrinkles and compression where the arms and product meet.
               - 'Opening/Using': Depict dynamic hand movements and precise fingertip action involved in opening a package. Add a sense of liveliness by slightly exposing the contents.

            3. Cinematic Quality & Environment
               - Unified Lighting: Even if the reference images have different lighting, apply a single, unified studio lighting setup to eliminate discrepancies in shadows and highlights.
               - Focus & Depth: Ensure the person's face and product logo are in sharp focus, while applying natural bokeh (background blur) to create depth.
               - Quality: Maintain 8K resolution, ultra-detailed textures, and a hyper-realistic commercial photography aesthetic.

            4. Anti-Hallucination & Physical Integrity
               - Anatomical Integrity: Strictly ensure the person has exactly 5 fingers. Prohibit abnormal joint angles or the generation of extra limbs (Negative constraint).
               - Anti-Floating: Ensure the product does not appear to be floating. Generate precise contact shadows and physical pressure at the points of interaction.
               - Anti-Distortion: Exclude any duplication of persons or objects. Prevent logo text corruption or any alteration of brand typography.

            5. Final Exclusions
               - Do not generate any artificial text, dates, or watermarks on the background or product unless explicitly requested by the user. Output only the visual composite.
        """;

    @Override
    public List<String> generateFusionImages(String userMessage,
                                             byte[] personImageBytes, String personMimeType,
                                             byte[] productImageBytes, String productMimeType,
                                             int count) {
        log.debug("NanoBanana 합성 이미지 {}개 병렬 생성 시작", count);

        String personBase64  = Base64.getEncoder().encodeToString(personImageBytes);
        String productBase64 = Base64.getEncoder().encodeToString(productImageBytes);

        NbRequest requestBody = new NbRequest(
                new NbSystemInstruction(List.of(new NbPart(FUSION_SYSTEM_PROMPT))),
                List.of(new NbContent("user", List.of(
                        new NbPart("Reference_Image_1 (인물):"),
                        new NbPart(null, new NbInlineData(personMimeType, personBase64)),
                        new NbPart("Reference_Image_2 (상품):"),
                        new NbPart(null, new NbInlineData(productMimeType, productBase64)),
                        new NbPart(userMessage)
                ))),
                new NbGenerationConfig(List.of("IMAGE"))
        );

        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> callAndSave(requestBody)))
                .toList();

        try {
            List<String> imageUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            log.info("NanoBanana 합성 이미지 {}개 병렬 생성 완료 — urls: {}", count, imageUrls);
            return imageUrls;
        } catch (CompletionException e) {
            if (e.getCause() instanceof BizException bizEx) throw bizEx;
            throw e;
        }
    }

    private ImageGenerationErrorCode resolveImageErrorCode(int statusCode) {
        return switch (statusCode) {
            case 429 -> ImageGenerationErrorCode.IMAGE_GENERATION_RATE_LIMITED;
            case 403 -> ImageGenerationErrorCode.IMAGE_GENERATION_AUTH_FAILED;
            case 500, 503, 504 -> ImageGenerationErrorCode.IMAGE_GENERATION_UNAVAILABLE;
            default -> ImageGenerationErrorCode.IMAGE_GENERATION_FAILED;
        };
    }

    /**
     * base64 인코딩된 이미지 데이터를 디코딩하여 파일로 저장하고 접근 URL을 반환한다.
     */
    private String saveImage(String base64Data, String mimeType) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            String ext = (mimeType != null && mimeType.contains("png")) ? "png" : "jpg";
            String filename = UUID.randomUUID() + "." + ext;

            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), imageBytes);

            return "/images/" + filename;
        } catch (IOException e) {
            log.error("NanoBanana 이미지 저장 실패", e);
            throw new BizException(ImageGenerationErrorCode.IMAGE_SAVE_FAILED);
        }
    }

    // ================================================================
    // Gemini 이미지 생성 API 요청/응답 내부 DTO
    // ================================================================

    private record NbRequest(
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("system_instruction") NbSystemInstruction systemInstruction,
            List<NbContent> contents,
            @JsonProperty("generationConfig") NbGenerationConfig generationConfig
    ) {
        // 기존 코드 호환 — systemInstruction 없는 생성자
        NbRequest(List<NbContent> contents, NbGenerationConfig generationConfig) {
            this(null, contents, generationConfig);
        }
    }

    private record NbSystemInstruction(List<NbPart> parts) {}

    private record NbGenerationConfig(
            @JsonProperty("responseModalities") List<String> responseModalities
    ) {}

    private record NbContent(String role, List<NbPart> parts) {}

    private record NbPart(
            String text,
            @JsonProperty("inlineData") NbInlineData inlineData
    ) {
        NbPart(String text) {
            this(text, null);
        }
    }

    private record NbInlineData(
            @JsonProperty("mimeType") String mimeType,
            String data
    ) {}

    private record NbResponse(List<NbCandidate> candidates) {}

    private record NbCandidate(NbContent content) {}
}

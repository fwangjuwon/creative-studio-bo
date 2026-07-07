package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.AspectRatioUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageUpscaleUtil;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    /** 이미지 생성 API 호출당 최대 대기 시간 — 초과 시 IMAGE_GENERATION_FAILED 처리 */
    private static final long IMAGE_GENERATION_TIMEOUT_SECONDS = 120;

    /**
     * 모든 이미지 생성 호출에 적용되는 생성 옵션 — Gemini 기본값(1K) 그대로 사용한다.
     * 초광각/초장축 비율을 모델에 직접 4K로 요청하면 고스팅·디테일 붕괴가 발생해(참조 이미지와 결합 시 특히 심함),
     * 모델은 항상 안전한 기본 해상도로 생성하고 목표 해상도는 {@link ImageUpscaleUtil}로 후처리 확대한다.
     */
    private static final NbGenerationConfig IMAGE_GENERATION_CONFIG =
            new NbGenerationConfig(List.of("IMAGE"));

    /** 생성 이미지 내 텍스트에 적용되는 폰트 라이선스 가드 — 모든 이미지 생성 경로에 공통 적용 */
    private static final String FONT_LICENSE_SYSTEM_PROMPT = """
            [Rule]
            If any text appears in the generated image, render it using only commercially free fonts
            (e.g. Google Fonts, Open Font License). Never use proprietary or paid fonts.
            """;

    private String generateImage(String prompt, String imageAspectRatio) {
        log.debug("NanoBanana 이미지 생성 호출 — model: {}, imageAspectRatio: {}",
                geminiProperties.getImageModel(), imageAspectRatio);

        NbRequest requestBody = new NbRequest(
                new NbSystemInstruction(List.of(new NbPart(FONT_LICENSE_SYSTEM_PROMPT))),
                List.of(new NbContent("user", List.of(new NbPart(prompt)))),
                IMAGE_GENERATION_CONFIG
        );

        return callAndSave(requestBody, imageAspectRatio);
    }

    private static final String TEXT_OVERLAY_SYSTEM_PROMPT = """
            [Role]
            You are a precise marketing image text compositor.
            Your only task is to add the specified text to an existing finished image without altering anything else.

            [Core Rules]
            1. PRESERVE the original image exactly — do not change composition, layout, colors, lighting, existing text, logos, or any visual elements.
            2. ADD only the text explicitly specified in the user message.
            3. Choose font style, color, size, and placement that harmonizes with the existing image mood and design, as if the text was part of the original design.
            4. Use only commercially free fonts (e.g. Google Fonts, Open Font License). Do not use proprietary or paid fonts.
            5. Render every character with exact spelling and correct typography.
            """;

    @Override
    public String generateImageMime(String prompt, byte[] imageBytes, String mimeType, String imageAspectRatio) {
        log.debug("NanoBanana 이미지 생성 호출 (참조 이미지 포함) — model: {}, imageAspectRatio: {}",
                geminiProperties.getImageModel(), imageAspectRatio);

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        NbRequest requestBody = new NbRequest(
                new NbSystemInstruction(List.of(new NbPart(FONT_LICENSE_SYSTEM_PROMPT))),
                List.of(new NbContent("user", List.of(
                        new NbPart(prompt),
                        new NbPart(null, new NbInlineData(mimeType, base64Image))
                ))),
                IMAGE_GENERATION_CONFIG
        );

        return callAndSave(requestBody, imageAspectRatio);
    }

    private String generateTextOverlayImage(String prompt, byte[] imageBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        NbRequest requestBody = new NbRequest(
                new NbSystemInstruction(List.of(new NbPart(TEXT_OVERLAY_SYSTEM_PROMPT))),
                List.of(new NbContent("user", List.of(
                        new NbPart(prompt),
                        new NbPart(null, new NbInlineData(mimeType, base64Image))
                ))),
                IMAGE_GENERATION_CONFIG
        );

        return callAndSave(requestBody, null);
    }

    @Override
    public List<String> generateTextOverlayImages(String prompt, byte[] imageBytes, String mimeType, int count) {
        log.debug("NanoBanana 텍스트 합성 이미지 {}개 병렬 생성 시작", count);

        CopyOnWriteArrayList<String> savedUrls = new CopyOnWriteArrayList<>();
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String url = generateTextOverlayImage(prompt, imageBytes, mimeType);
                    savedUrls.add(url);
                    return url;
                }).orTimeout(IMAGE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .toList();

        try {
            List<String> imageUrls = futures.stream().map(CompletableFuture::join).toList();
            log.info("NanoBanana 텍스트 합성 이미지 {}개 병렬 생성 완료 — urls: {}", count, imageUrls);
            return imageUrls;
        } catch (CompletionException e) {
            savedUrls.forEach(this::deleteImageFile);
            if (e.getCause() instanceof BizException bizEx) throw bizEx;
            if (e.getCause() instanceof TimeoutException) {
                log.error("NanoBanana 이미지 생성 타임아웃 ({}s 초과)", IMAGE_GENERATION_TIMEOUT_SECONDS);
                throw new BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED);
            }
            throw e;
        }
    }

    /**
     * Gemini 이미지 생성 API를 호출하고 응답에서 이미지를 추출·저장한 뒤 URL을 반환한다.
     * 극단 비율(초광각/초장축)은 태그 픽셀값 기준으로, 그 외는 2x 배율로 {@link ImageUpscaleUtil} 업스케일 후 저장한다.
     */
    private String callAndSave(NbRequest requestBody, String imageAspectRatio) {
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

        // Gemini는 텍스트 파트 + 이미지 파트 순으로 응답할 수 있으므로 전체 파트 탐색
        NbInlineData inlineData = content.parts().stream()
                .map(NbPart::inlineData)
                .filter(d -> d != null && d.data() != null && !d.data().isBlank())
                .findFirst()
                .orElse(null);
        if (inlineData == null) {
            log.error("NanoBanana 응답에 inlineData가 없습니다");
            throw new BizException(ImageGenerationErrorCode.EMPTY_IMAGE_RESPONSE);
        }

        byte[] imageBytes = Base64.getDecoder().decode(inlineData.data());
        // 극단 비율(초광각/초장축)은 태그에 저장된 정확한 픽셀 크기로, 그 외는 2x 배율로 업스케일
        int[] exactTarget = AspectRatioUtil.isWideRatio(imageAspectRatio) || AspectRatioUtil.isTallRatio(imageAspectRatio)
                ? AspectRatioUtil.parseTargetDimensions(imageAspectRatio)
                : null;
        if (exactTarget != null) {
            log.debug("극단 비율 — 정확한 목표 픽셀로 업스케일: {} -> {}x{}", imageAspectRatio, exactTarget[0], exactTarget[1]);
            imageBytes = ImageUpscaleUtil.upscale(imageBytes, inlineData.mimeType(), exactTarget[0], exactTarget[1]);
        } else {
            if (AspectRatioUtil.isWideRatio(imageAspectRatio) || AspectRatioUtil.isTallRatio(imageAspectRatio)) {
                log.warn("극단 비율이지만 parseTargetDimensions 실패 — 2x fallback 적용. aspectRatio: {}", imageAspectRatio);
            } else {
                log.debug("2x 업스케일 적용 — imageAspectRatio: {}", imageAspectRatio);
            }
            imageBytes = ImageUpscaleUtil.upscale(imageBytes, inlineData.mimeType(), 2.0);
        }

        String imageUrl = saveImage(imageBytes, inlineData.mimeType());
        log.debug("NanoBanana 이미지 저장 완료 — url: {}", imageUrl);

        return imageUrl;
    }

    @Override
    public List<String> generateImages(String prompt, int count, String imageAspectRatio) {
        log.debug("NanoBanana 이미지 {}개 병렬 생성 시작 — imageAspectRatio: {}", count, imageAspectRatio);

        // count개 CompletableFuture 생성 후 병렬 실행
        CopyOnWriteArrayList<String> savedUrls = new CopyOnWriteArrayList<>();
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String url = generateImage(prompt, imageAspectRatio);
                    savedUrls.add(url);
                    return url;
                }).orTimeout(IMAGE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .toList();

        try {
            List<String> imageUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            log.info("NanoBanana 이미지 {}개 병렬 생성 완료 — urls: {}", count, imageUrls);
            return imageUrls;
        } catch (CompletionException e) {
            savedUrls.forEach(this::deleteImageFile);
            if (e.getCause() instanceof BizException bizEx) throw bizEx;
            if (e.getCause() instanceof TimeoutException) {
                log.error("NanoBanana 이미지 생성 타임아웃 ({}s 초과)", IMAGE_GENERATION_TIMEOUT_SECONDS);
                throw new BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED);
            }
            throw e;
        }
    }

    private static final String FUSION_SYSTEM_PROMPT = """
            [Role]
            You are a conservative reference-image editing and compositing engine.
            This is not a free character generation task. The input person image is the identity source.

            [Priority Order]
            1. Preserve the exact person from Reference_Image_1.
            2. Preserve the exact product from Reference_Image_2.
            3. Follow the requested pose, action, background, and aspect ratio only when they do not conflict with 1 or 2.

            [Core Mission]
            Create one natural commercial-photo composite where the same person from Reference_Image_1 interacts with the same product from Reference_Image_2.
            If any user instruction would require changing the person's identity, ignore that part and keep the original identity.

            [Identity Preservation]
            - Treat Reference_Image_1 as the authoritative identity reference.
            - Keep the same face, facial proportions, eye shape, nose, lips, jawline, skin tone, visible marks, hairstyle, hair color, hairline, body proportions, and apparent age.
            - Do not beautify, average, stylize, recast, swap, de-age, age, or generate a similar-looking substitute.
            - Do not infer or change background, ethnicity, nationality, or demographic traits. Copy only the visible person-specific traits from the reference image.
            - The output must look like an edited photo of the input person, not a newly generated model inspired by that person.

            [Product Preservation]
            - Treat Reference_Image_2 as the authoritative product reference.
            - Preserve product shape, packaging, color, logo placement, typography, and visible design details.
            - Keep the product physically plausible in the person's hand, near the body, or in the requested interaction.
            - If Reference_Image_2 contains a mannequin, model, hanger, stand, background, or display body, use it only to understand the product. Do not copy that body, head, face, pose, or scene into the output.

            [Scale & Body Proportion]
            - Build the final scene around the person from Reference_Image_1, using their natural head-to-body ratio and body proportions.
            - Do not paste the person's face or head onto the body from Reference_Image_2.
            - Do not enlarge the head, shrink the body, or mix body parts between references.
            - Resize and position the product to fit the person's real body scale and the requested interaction.
            - For apparel, render the garment as worn by the person from Reference_Image_1, tailored to that person's body proportions, while preserving the garment's color, material, silhouette, and design details.
            - If the product is a garment photographed on another body or mannequin, remove the original wearer/mannequin and transfer only the garment onto the reference person.

            [Interaction & Physics]
            - Make contact points realistic: fingers wrap around product edges, pressure is visible where hands or arms touch the product, and contact shadows anchor the product.
            - Maintain normal anatomy: one person only, no duplicated faces, no extra limbs, no extra fingers, no broken joints.
            - Keep the person's face and the product/logo clear and in focus.

            [Style]
            - Use unified studio lighting and a clean commercial photography look.
            - Avoid artificial text, dates, watermarks, or unrelated background elements,
              unless text is explicitly requested in the user message below.
            - If any text is rendered, use only commercially free fonts (e.g. Google Fonts, Open Font License).
              Never use proprietary or paid fonts.
        """;

    @Override
    public List<String> generateFusionImages(String userMessage,
                                             byte[] personImageBytes, String personMimeType,
                                             byte[] productImageBytes, String productMimeType,
                                             int count, String imageAspectRatio) {
        log.debug("NanoBanana 합성 이미지 {}개 병렬 생성 시작 — imageAspectRatio: {}", count, imageAspectRatio);

        String personBase64  = Base64.getEncoder().encodeToString(personImageBytes);
        String productBase64 = Base64.getEncoder().encodeToString(productImageBytes);

        NbRequest requestBody = new NbRequest(
                new NbSystemInstruction(List.of(new NbPart(FUSION_SYSTEM_PROMPT))),
                List.of(new NbContent("user", List.of(
                        new NbPart("""
                                Reference_Image_1: PERSON IDENTITY SOURCE.
                                Use this exact person as the only allowed person in the output.
                                """),
                        new NbPart(null, new NbInlineData(personMimeType, personBase64)),
                        new NbPart("""
                                Reference_Image_2: PRODUCT SOURCE.
                                Use this exact product as the only allowed product in the output.
                                """),
                        new NbPart(null, new NbInlineData(productMimeType, productBase64)),
                        new NbPart("""
                                Task: Create a reference-based composite/edit, not a new person.
                                Final check before output: the face must still match Reference_Image_1 more than any generic model.
                                Also check scale: preserve the person's natural head-to-body proportions and fit the product to the person, never the person to the product image.
                                """),
                        new NbPart(userMessage)
                ))),
                IMAGE_GENERATION_CONFIG
        );

        CopyOnWriteArrayList<String> savedUrls = new CopyOnWriteArrayList<>();
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String url = callAndSave(requestBody, imageAspectRatio);
                    savedUrls.add(url);
                    return url;
                }).orTimeout(IMAGE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .toList();

        try {
            List<String> imageUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            log.info("NanoBanana 합성 이미지 {}개 병렬 생성 완료 — urls: {}", count, imageUrls);
            return imageUrls;
        } catch (CompletionException e) {
            savedUrls.forEach(this::deleteImageFile);
            if (e.getCause() instanceof BizException bizEx) throw bizEx;
            if (e.getCause() instanceof TimeoutException) {
                log.error("NanoBanana 이미지 생성 타임아웃 ({}s 초과)", IMAGE_GENERATION_TIMEOUT_SECONDS);
                throw new BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED);
            }
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

    @Override
    public void deleteImageFile(String relativePath) {
        String filename = Paths.get(relativePath).getFileName().toString();
        Path filePath = Paths.get(storagePath, filename);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("병렬 생성 실패 — 저장된 이미지 파일 정리 완료: {}", filePath);
            } else {
                log.warn("병렬 생성 실패 — 정리할 파일이 존재하지 않음: {}", filePath);
            }
        } catch (IOException e) {
            log.error("병렬 생성 실패 — 이미지 파일 정리 중 오류: {}", filePath, e);
        }
    }

    /**
     * 이미지 바이너리를 파일로 저장하고 접근 URL을 반환한다.
     */
    private String saveImage(byte[] imageBytes, String mimeType) {
        try {
            String ext = (mimeType != null && mimeType.equalsIgnoreCase("image/png")) ? "png" : "jpg";
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

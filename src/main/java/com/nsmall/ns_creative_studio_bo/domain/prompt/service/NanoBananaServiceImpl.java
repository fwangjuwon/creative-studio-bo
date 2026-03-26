package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

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

    @Value("${image.base-url}")
    private String imageBaseUrl;

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
        String uri = String.format("/models/%s:generateContent?key=%s",
                geminiProperties.getImageModel(), geminiProperties.getApiKey());

        NbResponse response = geminiRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(NbResponse.class);

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

            return imageBaseUrl + "/images/" + filename;
        } catch (IOException e) {
            log.error("NanoBanana 이미지 저장 실패", e);
            throw new BizException(ImageGenerationErrorCode.IMAGE_SAVE_FAILED);
        }
    }

    // ================================================================
    // Gemini 이미지 생성 API 요청/응답 내부 DTO
    // ================================================================

    private record NbRequest(
            List<NbContent> contents,
            @JsonProperty("generationConfig") NbGenerationConfig generationConfig
    ) {}

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

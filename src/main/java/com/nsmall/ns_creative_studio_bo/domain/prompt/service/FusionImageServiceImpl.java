package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 합성 이미지 생성 서비스 구현체
 *
 * <pre>
 * 처리 흐름:
 *   1. 상품/인물 이미지 유효성 검증 (ImageValidationUtil 공용)
 *   2. pose + instruction + imageAspectRatio로 user message 구성
 *   3. ImageGenerationService.generateFusionImages() 호출
 *   4. 상대 경로 → 절대 URL 변환 후 반환
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FusionImageServiceImpl implements FusionImageService {

    private final ImageGenerationService imageGenerationService;

    @Value("${image.base-url}")
    private String imageBaseUrl;

    @Override
    public List<String> generateFusionImages(MultipartFile productImage, MultipartFile personImage,
                                             String pose, String instruction, String imageAspectRatio) {
        ValidatedImage validatedProduct = ImageValidationUtil.validate(productImage);
        ValidatedImage validatedPerson  = ImageValidationUtil.validate(personImage);

        String userMessage = buildUserMessage(pose, instruction, imageAspectRatio);

        log.debug("합성 이미지 생성 요청 — pose={}, instruction={}, aspectRatio={}",
                pose, instruction, imageAspectRatio);

        List<String> imageUrls = imageGenerationService.generateFusionImages(
                userMessage,
                validatedPerson.bytes(),  validatedPerson.mimeType(),
                validatedProduct.bytes(), validatedProduct.mimeType(),
                PromptConstants.IMAGE_GENERATE_COUNT
        );

        return imageUrls.stream()
                .map(path -> imageBaseUrl + path)
                .toList();
    }

    /**
     * pose, instruction, imageAspectRatio를 조합하여 NanaBanana에 전달할 user message를 구성한다.
     */
    private String buildUserMessage(String pose, String instruction, String imageAspectRatio) {
        StringBuilder sb = new StringBuilder();
        if (pose != null && !pose.isBlank()) {
            sb.append("포즈/행동: ").append(pose.strip()).append("\n");
        }
        if (instruction != null && !instruction.isBlank()) {
            sb.append("추가 지시사항: ").append(instruction.strip()).append("\n");
        }
        if (imageAspectRatio != null && !imageAspectRatio.isBlank()) {
            sb.append("Aspect ratio ").append(imageAspectRatio).append(".");
        }
        String message = sb.toString().strip();
        return message.isBlank() ? "상품과 인물을 자연스럽게 합성해 주세요." : message;
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
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

        if (validatedProduct == null || validatedPerson == null) {
            throw new BizException(ImageGenerationErrorCode.FUSION_IMAGE_REQUIRED);
        }

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
     * pose, instruction, imageAspectRatio를 조합하여 NanoBanana에 전달할 user message를 구성한다.
     *
     * <pre>
     * 모든 요청에 인물 동일성 유지 요구를 prefix로 삽입한다.
     * 병렬로 여러 이미지를 독립 생성하는 특성상 매 요청마다 명시적으로 강제해야
     * 국적·민족·외형이 달라지는 변동성을 억제할 수 있다.
     * </pre>
     */
    private String buildUserMessage(String pose, String instruction, String imageAspectRatio) {
        StringBuilder sb = new StringBuilder();

        // 인물 동일성 유지 — 병렬 독립 생성 시 국적/외형 변동 억제
        sb.append("STRICT IDENTITY REQUIREMENT: ")
          .append("The person in the output MUST be the EXACT same individual as shown in Reference_Image_1. ")
          .append("Do NOT change their ethnicity, nationality, skin tone, facial features, hairstyle, or body type in any way. ")
          .append("Reproduce this specific person with 100% fidelity.\n\n");

        boolean hasSpecificInstruction = false;
        if (pose != null && !pose.isBlank()) {
            sb.append("포즈/행동: ").append(pose.strip()).append("\n");
            hasSpecificInstruction = true;
        }
        if (instruction != null && !instruction.isBlank()) {
            sb.append("추가 지시사항: ").append(instruction.strip()).append("\n");
            hasSpecificInstruction = true;
        }
        if (imageAspectRatio != null && !imageAspectRatio.isBlank()) {
            sb.append("Aspect ratio ").append(imageAspectRatio).append(".");
            hasSpecificInstruction = true;
        }
        if (!hasSpecificInstruction) {
            sb.append("상품과 인물을 자연스럽게 합성해 주세요.");
        }
        return sb.toString().strip();
    }
}

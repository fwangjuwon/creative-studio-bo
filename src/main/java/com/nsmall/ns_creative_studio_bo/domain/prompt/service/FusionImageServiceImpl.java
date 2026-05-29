package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptType;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.PromptHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 합성 이미지 생성 서비스 구현체
 *
 * <pre>
 * 처리 흐름:
 *   1. 일일 한도 체크 (PromptHelper)
 *   2. 상품/인물 이미지 유효성 검증 (ImageValidationUtil 공용)
 *   3. pose + instruction + imageAspectRatio로 user message 구성
 *   4. ImageGenerationService.generateFusionImages() 호출
 *   5. PromptHistory 저장
 *   6. 상대 경로 → 절대 URL 변환 후 반환 (PromptHelper)
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
    private final PromptHistoryMapper promptHistoryMapper;
    private final PromptHelper promptHelper;

    @Override
    @Transactional
    public List<String> generateFusionImages(Long userId, MultipartFile productImage, MultipartFile personImage,
                                             String pose, String instruction, String imageAspectRatio) {
        // 1. 사용자 조회 + 일일 한도 체크
        promptHelper.checkDailyLimit(userId);

        // 2. 이미지 유효성 검증
        ValidatedImage validatedProduct = ImageValidationUtil.validate(productImage);
        ValidatedImage validatedPerson  = ImageValidationUtil.validate(personImage);

        if (validatedProduct == null || validatedPerson == null) {
            throw new BizException(ImageGenerationErrorCode.FUSION_IMAGE_REQUIRED);
        }

        // 3. userMessage 구성 + 이미지 생성 (상대 경로 반환)
        String userMessage = buildUserMessage(pose, instruction, imageAspectRatio);

        log.debug("합성 이미지 생성 요청 — pose={}, instruction={}, aspectRatio={}",
                pose, instruction, imageAspectRatio);

        List<String> imageUrls = imageGenerationService.generateFusionImages(
                userMessage,
                validatedPerson.bytes(),  validatedPerson.mimeType(),
                validatedProduct.bytes(), validatedProduct.mimeType(),
                PromptConstants.IMAGE_GENERATE_COUNT
        );

        // 4. PromptHistory 저장
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();
        String imageUrlsJson = promptHelper.serializeImageUrls(imageUrls);

        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .promptType(PromptType.FUSION.name())
                .promptStyle(PromptStyle.NANO_BANANA.name())
                .promptText(userMessage)
                .imageUrl(imageUrlsJson)
                .imageAspectRatio(imageAspectRatio)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 5. 절대 URL로 변환 후 반환
        return promptHelper.toAbsoluteUrls(imageUrls);
    }

    /**
     * pose, instruction, imageAspectRatio를 조합하여 NanoBanana에 전달할 user message를 구성한다.
     *
     * <pre>
     * identity/product 보존 규칙은 NanoBananaServiceImpl의 system_instruction에 두고,
     * 여기서는 사용자가 선택한 합성 지시만 전달한다.
     * </pre>
     */
    private String buildUserMessage(String pose, String instruction, String imageAspectRatio) {
        StringBuilder sb = new StringBuilder();

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
            sb.append("Create a natural commercial-photo composite of the exact person and exact product from the reference images.");
        }
        return sb.toString().strip();
    }
}

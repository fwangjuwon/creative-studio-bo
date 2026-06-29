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
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.TextOverlayUtil;
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
 * 처리 흐름 (인물+상품 합성):
 *   1. 일일 한도 체크 (PromptHelper)
 *   2. 상품/인물 이미지 유효성 검증 (ImageValidationUtil 공용)
 *   3. pose + instruction + imageAspectRatio로 user message 구성
 *   4. ImageGenerationService.generateFusionImages() 호출
 *   5. PromptHistory 저장
 *   6. 상대 경로 → 절대 URL 변환 후 반환 (PromptHelper)
 *
 * 처리 흐름 (텍스트 합성):
 *   1. 일일 한도 체크
 *   2. 단일 이미지 유효성 검증
 *   3. 텍스트 합성 프롬프트 구성
 *   4. generateTextOverlayImages() 위임 (N개 병렬은 NanoBananaServiceImpl에서 처리)
 *   5. PromptHistory 저장
 *   6. 절대 URL 변환 후 반환
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FusionImageServiceImpl implements FusionImageService {

    private final ImageGenerationService imageGenerationService;
    private final PromptHistoryMapper promptHistoryMapper;
    private final PromptHelper promptHelper;

    @Override
    @Transactional
    public List<String> generateFusionImages(Long userId, MultipartFile productImage, MultipartFile personImage,
                                             String pose, String instruction, String imageAspectRatio,
                                             String textContent, String disclaimerText) {
        // 1. 사용자 조회 + 일일 한도 체크
        promptHelper.checkDailyLimit(userId);

        // 2. 이미지 유효성 검증
        ValidatedImage validatedProduct = ImageValidationUtil.validate(productImage);
        ValidatedImage validatedPerson  = ImageValidationUtil.validate(personImage);

        if (validatedProduct == null || validatedPerson == null) {
            throw new BizException(ImageGenerationErrorCode.FUSION_IMAGE_REQUIRED);
        }

        // 3. userMessage 구성 + 이미지 생성 (상대 경로 반환)
        String userMessage = buildUserMessage(pose, instruction, imageAspectRatio, textContent, disclaimerText);

        log.debug("합성 이미지 생성 요청 — pose={}, instruction={}, aspectRatio={}, textContent={}",
                pose, instruction, imageAspectRatio, textContent);

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
                .textContent(textContent)
                .disclaimerText(disclaimerText)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 5. 절대 URL로 변환 후 반환
        return promptHelper.toAbsoluteUrls(imageUrls);
    }

    @Override
    @Transactional
    public List<String> generateTextOverlayImages(Long userId, MultipartFile image,
                                                  String textContent, String disclaimerText) {
        // 1. 일일 한도 체크
        promptHelper.checkDailyLimit(userId);

        // 2. 이미지 유효성 검증
        ValidatedImage validatedImage = ImageValidationUtil.validate(image);
        if (validatedImage == null) {
            throw new BizException(ImageGenerationErrorCode.TEXT_OVERLAY_IMAGE_REQUIRED);
        }

        // 3. 텍스트 합성 프롬프트 구성
        String prompt = buildTextOverlayMessage(textContent, disclaimerText);

        log.debug("텍스트 합성 이미지 생성 요청 — textContent={}, disclaimerText={}", textContent, disclaimerText);

        // 4. N개 병렬 생성 위임 (병렬 처리 및 부분 실패 시 파일 정리는 NanoBananaServiceImpl에서 처리)
        List<String> imageUrls = imageGenerationService.generateTextOverlayImages(
                prompt, validatedImage.bytes(), validatedImage.mimeType(), PromptConstants.IMAGE_GENERATE_COUNT);

        // 5. PromptHistory 저장
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .promptType(PromptType.TEXT_OVL.name())
                .promptStyle(PromptStyle.NANO_BANANA.name())
                .promptText(prompt)
                .imageUrl(promptHelper.serializeImageUrls(imageUrls))
                .textContent(textContent)
                .disclaimerText(disclaimerText)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 6. 절대 URL 변환 후 반환
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
    private String buildUserMessage(String pose, String instruction, String imageAspectRatio,
                                    String textContent, String disclaimerText) {
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

        String textSuffix = TextOverlayUtil.buildSuffix(textContent, disclaimerText);
        if (textSuffix != null) {
            sb.append(" ").append(textSuffix);
        }

        return sb.toString().strip();
    }

    /**
     * 텍스트 합성 모드용 NanoBanana user message를 구성한다.
     * 시스템 프롬프트(이미지 보존 지시)는 NanoBananaServiceImpl에 있으므로
     * 여기서는 "어떤 텍스트를 어떻게 배치할지"만 명시한다.
     */
    private String buildTextOverlayMessage(String textContent, String disclaimerText) {
        boolean hasMain = textContent != null && !textContent.isBlank();
        boolean hasDisclaimer = disclaimerText != null && !disclaimerText.isBlank();

        if (!hasMain && !hasDisclaimer) {
            throw new BizException(ImageGenerationErrorCode.TEXT_OVERLAY_TEXT_REQUIRED);
        }
        if (hasMain && hasDisclaimer) {
            return buildBothMessage(textContent.strip(), disclaimerText.strip());
        }
        if (hasMain) {
            return buildMainOnlyMessage(textContent.strip());
        }
        return buildDisclaimerOnlyMessage(disclaimerText.strip());
    }

    private String buildMainOnlyMessage(String text) {
        TextOverlayUtil.EmphasisResult emphasis = TextOverlayUtil.parseEmphasis(text);
        String display = emphasis.displayText().replace("\n", " / ");
        String lineBreakHint = emphasis.displayText().contains("\n")
                ? " Each ' / ' marks a suggested line break, but feel free to adjust the line layout as described below if a different breakdown looks better."
                : "";
        return "Add the main text: \"" + display + "\"" + lineBreakHint
                + " Ensure exact spelling is rendered correctly."
                + TextOverlayUtil.buildBroadcastStyleHint()
                + TextOverlayUtil.buildEmphasisHint(emphasis.emphasizedWords());
    }

    private String buildDisclaimerOnlyMessage(String text) {
        String display = text.replace("\n", " / ");
        String lineBreakHint = text.contains("\n")
                ? " Each ' / ' represents a line break — render each segment on a separate line."
                : "";
        return "Add the disclaimer text: \"" + display + "\"" + lineBreakHint
                + " Placement: identify the largest/most prominent text element already visible in the image (the main headline). "
                + "Place the disclaimer text DIRECTLY BELOW that headline text, with a small gap between them — "
                + "NOT at the very bottom of the image frame. "
                + "Align it to match the horizontal alignment of the existing headline text (left-aligned if headline is left-aligned, etc.). "
                + "Size: clearly smaller than the headline but large enough to be comfortably readable — "
                + "roughly 20–28% of the headline font size. "
                + "Color and font style: complement the existing headline text. "
                + "Ensure exact spelling is rendered correctly.";
    }

    private String buildBothMessage(String mainText, String disclaimerText) {
        TextOverlayUtil.EmphasisResult emphasis = TextOverlayUtil.parseEmphasis(mainText);
        String mainDisplay = emphasis.displayText().replace("\n", " / ");
        String mainHint = emphasis.displayText().contains("\n")
                ? " Each ' / ' marks a suggested line break, but feel free to adjust the line layout as described below if a different breakdown looks better."
                : "";
        String disclaimerDisplay = disclaimerText.replace("\n", " / ");
        String disclaimerHint = disclaimerText.contains("\n")
                ? " Each ' / ' represents a line break — render each segment on a separate line."
                : "";
        return "Add the following two text elements: "
                + "1. Main text: \"" + mainDisplay + "\"" + mainHint
                + TextOverlayUtil.buildBroadcastStyleHint()
                + TextOverlayUtil.buildEmphasisHint(emphasis.emphasizedWords())
                + " 2. Disclaimer text: \"" + disclaimerDisplay + "\"" + disclaimerHint
                + " — place DIRECTLY BELOW the main text added in step 1, with a small gap between them. "
                + "NOT at the very bottom of the image frame — it should sit just under the main text. "
                + "Align it to match the horizontal alignment of the main text. "
                + "Size: clearly smaller than the main text but comfortably readable — roughly 20–28% of the main text font size. "
                + "Color and font style: complement the main text. "
                + "Ensure exact spelling of both texts is rendered correctly.";
    }

}

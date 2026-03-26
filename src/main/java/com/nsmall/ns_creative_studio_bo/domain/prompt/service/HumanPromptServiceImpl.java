package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptHistoryOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptAttrMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptHistoryOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptStepMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 인간 이미지 프롬프트 서비스 구현체
 *
 * <pre>
 * 프롬프트 생성 흐름:
 *   1. 사용자 조회 + 일일 한도 체크
 *   2. 선택된 모든 optionId를 플랫 목록으로 수집
 *   3. 필수(is_required) attr 선택 여부 검증
 *   4. selectByIds(ids) 로 option 정보 + step_number 조회
 *   5. stepNumber 기준 Map 구성
 *   6. GeminiService.generateHumanPrompt() 호출
 *   7. PromptHistory 저장 (promptType='PERSON', categoryId=null)
 *   8. HumanPromptHistoryOption 저장 (선택 옵션 매핑)
 *   9. 생성된 이력 DTO 반환
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HumanPromptServiceImpl implements HumanPromptService {

    private final HumanPromptStepMapper humanPromptStepMapper;
    private final HumanPromptAttrMapper humanPromptAttrMapper;
    private final HumanPromptOptionMapper humanPromptOptionMapper;
    private final HumanPromptHistoryOptionMapper humanPromptHistoryOptionMapper;
    private final PromptHistoryMapper promptHistoryMapper;
    private final UserMapper userMapper;
    private final GeminiService geminiService;
    private final ImageGenerationService imageGenerationService;
    private final ObjectMapper objectMapper;

    @Value("${image.base-url}")
    private String imageBaseUrl;

    @Override
    public List<HumanPromptStepRespDto> getSteps() {
        return humanPromptStepMapper.selectAllWithAttrsAndOptions();
    }

    @Override
    @Transactional
    public PromptHistoryRespDto createPrompt(Long userId, HumanPromptCreateReqDto dto) {
        // 1. 사용자 조회 + 일일 한도 체크
        User user = userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(PromptErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + userId));

        int todayCount = promptHistoryMapper.countTodayByUserId(userId);
        if (todayCount >= user.getDailyLimit()) {
            log.warn("일일 한도 초과 — userId={}, todayCount={}, dailyLimit={}",
                    userId, todayCount, user.getDailyLimit());
            throw new BizException(PromptErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        // 2. 선택된 모든 optionId 플랫 목록 수집
        List<Long> allOptionIds = dto.getSelections().values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        if (allOptionIds.isEmpty()) {
            throw new BizException(HumanPromptErrorCode.NO_OPTIONS_SELECTED);
        }

        // 3. 필수 attr 선택 여부 검증
        List<Long> requiredAttrIds = humanPromptAttrMapper.selectRequiredAttrIds();
        boolean hasAllRequired = requiredAttrIds.stream()
                .allMatch(attrId -> {
                    List<Long> selected = dto.getSelections().get(attrId);
                    return selected != null && !selected.isEmpty();
                });
        if (!hasAllRequired) {
            log.warn("필수 항목 미선택 — userId={}, selections={}", userId, dto.getSelections());
            throw new BizException(HumanPromptErrorCode.REQUIRED_OPTION_MISSING);
        }

        // 4. option 정보 + step_number 조회
        List<HumanPromptOption> options = humanPromptOptionMapper.selectByIds(allOptionIds);

        // 5. stepNumber 기준 Map<stepNumber, List<promptValue>> 구성
        Map<Integer, List<String>> stepOptionValues = new HashMap<>();
        for (HumanPromptOption option : options) {
            stepOptionValues
                    .computeIfAbsent(option.getStepNumber(), k -> new ArrayList<>())
                    .add(option.getPromptValue());
        }

        log.debug("인간 이미지 프롬프트 생성 — stepOptionValues: {}", stepOptionValues);

        // 6. Gemini 호출 (promptStyle 기본값: NANO_BANANA)
        String style = (dto.getPromptStyle() != null && !dto.getPromptStyle().isBlank())
                ? dto.getPromptStyle() : "NANO_BANANA";
        String promptText = geminiService.generateHumanPrompt(stepOptionValues, dto.getPurpose(), dto.getAdditionalNotes(), style, dto.getImageAspectRatio());

        // 7. PromptHistory 저장 (PERSON 타입, categoryId=null)
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(null)
                .promptType("PERSON")
                .promptStyle(style)
                .promptText(promptText)
                .imageAspectRatio(dto.getImageAspectRatio())
                .imageUrl(null)
                .purpose(dto.getPurpose())
                .additionalNotes(dto.getAdditionalNotes())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 8. HumanPromptHistoryOption 저장 (선택 옵션 매핑)
        List<HumanPromptHistoryOption> historyOptions = allOptionIds.stream()
                .map(optionId -> HumanPromptHistoryOption.builder()
                        .promptHistoryId(history.getId())
                        .optionId(optionId)
                        .initRegiPic(requesterId)
                        .initRegiDttm(now)
                        .finalRvisPic(requesterId)
                        .finalRvisDttm(now)
                        .build())
                .toList();
        humanPromptHistoryOptionMapper.insertBatch(historyOptions);

        log.info("인물 프롬프트 이력 저장 완료 — userId={}, historyId={}, optionCount={}",
                userId, history.getId(), historyOptions.size());

        // 9. 응답 반환
        List<String> savedOptionNames = humanPromptHistoryOptionMapper.selectOptionNamesByHistoryId(history.getId());
        return PromptHistoryRespDto.fromHuman(history, savedOptionNames, imageBaseUrl);
    }

    @Override
    @Transactional
    public List<String> generateImage(Long historyId, String promptText) {
        // 1. 이미지 3개 병렬 생성
        List<String> imageUrls = imageGenerationService.generateImages(promptText, 3);

        // 2. 이력에 상대 경로 JSON 배열로 저장 (image_url 컬럼에 "[/images/uuid.jpg, ...]" 형태)
        if (historyId != null) {
            try {
                String imageUrlsJson = objectMapper.writeValueAsString(imageUrls);
                promptHistoryMapper.updateImageUrl(historyId, imageUrlsJson, SecurityUtil.getCurrentLoginId());
                log.info("이미지 URL 이력 업데이트 완료 — historyId={}, urls={}", historyId, imageUrlsJson);
            } catch (JsonProcessingException e) {
                log.warn("이미지 URL JSON 직렬화 실패 — 첫 번째 URL만 저장. historyId={}", historyId, e);
                promptHistoryMapper.updateImageUrl(historyId, imageUrls.get(0), SecurityUtil.getCurrentLoginId());
            }
        }

        // 3. 응답에는 절대 URL로 조립하여 반환
        return imageUrls.stream()
                .map(path -> imageBaseUrl + path)
                .toList();
    }
}

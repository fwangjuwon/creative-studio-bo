package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptStepMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *   3. selectByIds(ids) 로 option 정보 + step_number 조회
 *   4. stepNumber 기준 Map 구성
 *   5. GeminiService.generateHumanPrompt() 호출
 *   6. PromptHistory 저장 (promptType='PERSON', categoryId=null)
 *   7. 생성된 이력 DTO 반환
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
    private final HumanPromptOptionMapper humanPromptOptionMapper;
    private final PromptHistoryMapper promptHistoryMapper;
    private final UserMapper userMapper;
    private final GeminiService geminiService;
    private final ImageGenerationService imageGenerationService;

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

        // 3. option 정보 + step_number 조회
        List<HumanPromptOption> options = humanPromptOptionMapper.selectByIds(allOptionIds);

        // 4. stepNumber 기준 Map<stepNumber, List<promptValue>> 구성
        Map<Integer, List<String>> stepOptionValues = new HashMap<>();
        for (HumanPromptOption option : options) {
            stepOptionValues
                    .computeIfAbsent(option.getStepNumber(), k -> new ArrayList<>())
                    .add(option.getPromptValue());
        }

        log.debug("인간 이미지 프롬프트 생성 — stepOptionValues: {}", stepOptionValues);

        // 5. Gemini 호출 (promptStyle 기본값: NANO_BANANA)
        String style = (dto.getPromptStyle() != null && !dto.getPromptStyle().isBlank())
                ? dto.getPromptStyle() : "NANO_BANANA";
        String promptText = geminiService.generateHumanPrompt(stepOptionValues, dto.getPurpose(), dto.getAdditionalNotes(), style);

        // 6. PromptHistory 저장 (PERSON 타입, categoryId=null, imageUrl=null)
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(null)
                .promptType("PERSON")
                .promptText(promptText)
                .imageUrl(null)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        log.info("인물 프롬프트 이력 저장 완료 — userId={}, historyId={}", userId, history.getId());

        return PromptHistoryRespDto.from(history, List.of());
    }

    @Override
    @Transactional
    public String generateImage(Long historyId, String promptText) {
        // 1. 이미지 생성
        String imageUrl = imageGenerationService.generateImage(promptText);

        // 2. 이력에 imageUrl 업데이트
        if (historyId != null) {
            promptHistoryMapper.updateImageUrl(historyId, imageUrl);
            log.info("이미지 URL 이력 업데이트 완료 — historyId={}, imageUrl={}", historyId, imageUrl);
        }

        return imageUrl;
    }
}

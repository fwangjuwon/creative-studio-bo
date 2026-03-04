package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 인간 이미지 프롬프트 서비스 구현체
 *
 * <pre>
 * 프롬프트 생성 흐름:
 *   1. 선택된 모든 optionId를 플랫 목록으로 수집
 *   2. selectByIds(ids) 로 option 정보 + step_number 조회
 *   3. stepNumber 기준 Map 구성
 *   4. GeminiService.generateHumanPrompt() 호출
 *   5. 생성된 promptText 반환 (저장 없음)
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
    private final GeminiService geminiService;

    @Override
    public List<HumanPromptStepRespDto> getSteps() {
        return humanPromptStepMapper.selectAllWithAttrsAndOptions();
    }

    @Override
    public String createPrompt(HumanPromptCreateReqDto dto) {
        // 1. 선택된 모든 optionId 플랫 목록 수집
        List<Long> allOptionIds = dto.getSelections().values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        if (allOptionIds.isEmpty()) {
            throw new BizException(HumanPromptErrorCode.NO_OPTIONS_SELECTED);
        }

        // 2. option 정보 + step_number 조회
        List<HumanPromptOption> options = humanPromptOptionMapper.selectByIds(allOptionIds);

        // 3. stepNumber 기준 Map<stepNumber, List<promptValue>> 구성
        Map<Integer, List<String>> stepOptionValues = new HashMap<>();
        for (HumanPromptOption option : options) {
            stepOptionValues
                    .computeIfAbsent(option.getStepNumber(), k -> new ArrayList<>())
                    .add(option.getPromptValue());
        }

        log.debug("인간 이미지 프롬프트 생성 — stepOptionValues: {}", stepOptionValues);

        // 4. Gemini 호출
        return geminiService.generateHumanPrompt(stepOptionValues, dto.getPurpose(), dto.getAdditionalNotes());
    }
}

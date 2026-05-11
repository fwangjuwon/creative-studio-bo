package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 인간 이미지 프롬프트 서비스 구현체
 *
 * <pre>
 * 프롬프트 생성 흐름:
 *   1. 사용자 조회 + 일일 한도 체크
 *   2. 모드 판별 — isGroup(6명 이상 단체) vs persons(1~5명 개별)
 *   3. 공통 step 필수 attr 검증 + 개별 step 필수 attr 인물별 검증
 *   4. 공통 옵션(is_common=true) → stepName 기준 Map 구성
 *   5. 인물별 옵션(is_common=false) → 각 인물별 stepName 기준 Map 구성
 *   6. GeminiService.generateHumanPrompt() 호출
 *   7. PromptHistory 저장 (promptType='PERSON', categoryId=null)
 *   8. HumanPromptHistoryOption 저장 (personIndex 포함 배치 insert)
 *      - 공통 옵션: personIndex = -1
 *      - 인물별 옵션: personIndex = 0~N
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

        // 2. 모드 판별 — 6명 이상(단체) vs 1~5명(개별)
        boolean isGroup = Boolean.TRUE.equals(dto.getIsGroup());

        List<Map<Long, List<Long>>> allPersonsSelections;
        if (isGroup) {
            if (dto.getSelections() == null || dto.getSelections().isEmpty()) {
                throw new BizException(HumanPromptErrorCode.NO_OPTIONS_SELECTED);
            }
            allPersonsSelections = List.of(dto.getSelections());
        } else {
            if (dto.getPersons() == null || dto.getPersons().isEmpty()) {
                throw new BizException(HumanPromptErrorCode.NO_OPTIONS_SELECTED);
            }
            allPersonsSelections = dto.getPersons().stream()
                    .map(p -> p.getSelections())
                    .toList();
        }

        // 3. 필수 attr 선택 여부 검증 — 공통/개별 step 분리 검증
        // 3-1. 공통 step 필수 attr 검증 (조명/촬영)
        List<Long> commonRequiredAttrIds = humanPromptAttrMapper.selectRequiredAttrIdsByStepIsCommon(true);
        Map<Long, List<Long>> commonSelectionsMap = dto.getCommonSelections() != null ? dto.getCommonSelections() : Map.of();
        boolean hasAllCommonRequired = commonRequiredAttrIds.stream()
                .allMatch(attrId -> {
                    List<Long> selected = commonSelectionsMap.get(attrId);
                    return selected != null && !selected.isEmpty();
                });
        if (!hasAllCommonRequired) {
            log.warn("공통 필수 항목 미선택 — userId={}", userId);
            throw new BizException(HumanPromptErrorCode.REQUIRED_OPTION_MISSING);
        }

        // 3-2. 개별 step 필수 attr 검증 (피사체/분위기/리얼리즘)
        List<Long> individualRequiredAttrIds = humanPromptAttrMapper.selectRequiredAttrIdsByStepIsCommon(false);
        for (int i = 0; i < allPersonsSelections.size(); i++) {
            Map<Long, List<Long>> selections = allPersonsSelections.get(i);
            boolean hasAllRequired = individualRequiredAttrIds.stream()
                    .allMatch(attrId -> {
                        List<Long> selected = selections.get(attrId);
                        return selected != null && !selected.isEmpty();
                    });
            if (!hasAllRequired) {
                String label = isGroup ? "공통 옵션" : "사람 " + (i + 1);
                log.warn("필수 항목 미선택 — userId={}, {}", userId, label);
                throw new BizException(HumanPromptErrorCode.REQUIRED_OPTION_MISSING);
            }
        }

        // 4. 공통 옵션(조명/촬영) 조회 → stepName 기준 Map 구성 + HistoryOption 버퍼에 추가
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        List<HumanPromptHistoryOption> historyOptionsBuf = new ArrayList<>();
        Map<String, List<String>> commonStepValues = new LinkedHashMap<>();

        if (dto.getCommonSelections() != null && !dto.getCommonSelections().isEmpty()) {
            List<Long> commonOptionIds = dto.getCommonSelections().values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
            List<HumanPromptOption> commonOptions = humanPromptOptionMapper.selectByIds(commonOptionIds);
            for (HumanPromptOption option : commonOptions) {
                commonStepValues.computeIfAbsent(option.getStepName(), k -> new ArrayList<>())
                        .add(option.getPromptValue());
            }
            // 공통 옵션은 personIndex = -1 로 구분하여 저장
            for (Long optionId : commonOptionIds) {
                historyOptionsBuf.add(HumanPromptHistoryOption.builder()
                        .optionId(optionId)
                        .personIndex(-1)
                        .initRegiPic(requesterId)
                        .initRegiDttm(now)
                        .finalRvisPic(requesterId)
                        .finalRvisDttm(now)
                        .build());
            }
        }

        // 5. 인물별 optionId 조회 → stepName 기준 Map 구성 + HistoryOption 레코드 준비
        List<Map<String, List<String>>> personsStepValues = new ArrayList<>();

        for (int personIdx = 0; personIdx < allPersonsSelections.size(); personIdx++) {
            Map<Long, List<Long>> selections = allPersonsSelections.get(personIdx);
            List<Long> optionIds = selections.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .toList();

            if (optionIds.isEmpty()) {
                throw new BizException(HumanPromptErrorCode.NO_OPTIONS_SELECTED);
            }

            List<HumanPromptOption> options = humanPromptOptionMapper.selectByIds(optionIds);

            Map<String, List<String>> stepValues = new LinkedHashMap<>();
            for (HumanPromptOption option : options) {
                stepValues.computeIfAbsent(option.getStepName(), k -> new ArrayList<>())
                        .add(option.getPromptValue());
            }
            personsStepValues.add(stepValues);

            final int idx = personIdx;
            for (Long optionId : optionIds) {
                historyOptionsBuf.add(HumanPromptHistoryOption.builder()
                        .optionId(optionId)
                        .personIndex(idx)
                        .initRegiPic(requesterId)
                        .initRegiDttm(now)
                        .finalRvisPic(requesterId)
                        .finalRvisDttm(now)
                        .build());
            }
        }

        log.debug("인물 프롬프트 생성 — personCount={}, isGroup={}, commonSteps={}",
                personsStepValues.size(), isGroup, commonStepValues.keySet());

        // 6. Gemini 호출
        PromptStyle style = dto.getPromptStyle() != null ? dto.getPromptStyle() : PromptStyle.NANO_BANANA;
        String promptText = geminiService.generateHumanPrompt(
                personsStepValues, commonStepValues, isGroup,
                dto.getRelationship(), dto.getAdditionalNotes(), style, dto.getImageAspectRatio());

        // 7. PromptHistory 저장
        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(null)
                .promptType(PromptType.PERSON.name())
                .promptStyle(style.name())
                .promptText(promptText)
                .imageAspectRatio(dto.getImageAspectRatio())
                .imageUrl(null)
                .purpose(null)
                .additionalNotes(dto.getAdditionalNotes())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 8. HumanPromptHistoryOption 저장 (promptHistoryId 주입 후 배치 insert)
        List<HumanPromptHistoryOption> historyOptions = historyOptionsBuf.stream()
                .map(o -> HumanPromptHistoryOption.builder()
                        .promptHistoryId(history.getId())
                        .optionId(o.getOptionId())
                        .personIndex(o.getPersonIndex())
                        .initRegiPic(o.getInitRegiPic())
                        .initRegiDttm(o.getInitRegiDttm())
                        .finalRvisPic(o.getFinalRvisPic())
                        .finalRvisDttm(o.getFinalRvisDttm())
                        .build())
                .toList();
        humanPromptHistoryOptionMapper.insertBatch(historyOptions);

        log.info("인물 프롬프트 이력 저장 완료 — userId={}, historyId={}, personCount={}, optionCount={}",
                userId, history.getId(), personsStepValues.size(), historyOptions.size());

        // 9. 응답 반환 (생성 직후 응답은 personOptions 빈 목록 — 이력 조회 시 상세 표시)
        return PromptHistoryRespDto.fromHuman(history, List.of(), imageBaseUrl);
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

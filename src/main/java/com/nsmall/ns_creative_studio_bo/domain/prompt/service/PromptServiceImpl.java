package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import com.nsmall.ns_creative_studio_bo.domain.category.mapper.CategoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistoryTag;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryTagMapper;
import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 서비스 구현체
 *
 * <pre>
 * 프롬프트 생성 흐름:
 *   1. 사용자 존재 확인 + 일일 한도 체크
 *   2. 카테고리 존재 확인 + 태그 존재 확인
 *   3. Gemini API 호출 → 영문 프롬프트 생성
 *   4. PromptHistory + PromptHistoryTag 저장
 *   5. 응답 반환
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final PromptHistoryMapper promptHistoryMapper;
    private final PromptHistoryTagMapper promptHistoryTagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final GeminiService geminiService;

    @Override
    @Transactional
    public PromptHistoryRespDto createGoodsPrompt(Long currentUserId, GoodsPromptCreateReqDto dto) {
        // 1. 사용자 조회 + 일일 한도 체크
        User user = userMapper.selectById(currentUserId)
                .orElseThrow(() -> new BizException(PromptErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + currentUserId));

        int todayCount = promptHistoryMapper.countTodayByUserId(currentUserId);
        if (todayCount >= user.getDailyLimit()) {
            log.warn("일일 한도 초과 — userId={}, todayCount={}, dailyLimit={}",
                    currentUserId, todayCount, user.getDailyLimit());
            throw new BizException(PromptErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        // 2. 상품 카테고리 조회 + DCS 검증
        Category category = categoryMapper.selectById(dto.getGoodsCategoryId())
                .orElseThrow(() -> new BizException(PromptErrorCode.CATEGORY_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다. categoryId=" + dto.getGoodsCategoryId()));

        if (CategoryType.DCS != category.getCategoryType()) {
            throw new BizException(PromptErrorCode.INVALID_CATEGORY_TYPE,
                    "DCS 카테고리가 아닙니다. categoryType=" + category.getCategoryType());
        }

        // DCS 이름만 보내면 맥락이 불분명하므로 LCS→DCS 전체 경로를 조합하여 전달
        List<String> categoryPath = categoryMapper.selectCategoryPath(dto.getGoodsCategoryId());
        String goodsCategoryName = String.join(" > ", categoryPath);

        // 3. 태그 조회
        List<String> tagNames = dto.getTagIds().stream()
                .map(tagId -> tagMapper.selectById(tagId)
                        .map(Tag::getName)
                        .orElseThrow(() -> new BizException(PromptErrorCode.TAG_NOT_FOUND,
                                "태그를 찾을 수 없습니다. tagId=" + tagId)))
                .toList();

        // 4. Gemini API 호출
        String promptText = geminiService.generateGoodsPrompt(goodsCategoryName, tagNames);

        // 5. PromptHistory 저장
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        PromptHistory history = PromptHistory.builder()
                .userId(currentUserId)
                .categoryId(dto.getGoodsCategoryId())
                .promptType("PRODUCT")
                .promptText(promptText)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 6. PromptHistoryTag 저장 (태그가 있는 경우만)
        if (!dto.getTagIds().isEmpty()) {
            List<PromptHistoryTag> historyTags = dto.getTagIds().stream()
                    .map(tagId -> PromptHistoryTag.builder()
                            .promptHistoryId(history.getId())
                            .tagId(tagId)
                            .initRegiPic(requesterId)
                            .initRegiDttm(now)
                            .finalRvisPic(requesterId)
                            .finalRvisDttm(now)
                            .build())
                    .toList();

            promptHistoryTagMapper.insertBatch(historyTags);
        }

        // 7. 응답 반환
        List<Long> savedTagIds = promptHistoryTagMapper.selectTagIdsByPromptHistoryId(history.getId());
        return PromptHistoryRespDto.from(history, savedTagIds);
    }

    @Override
    public List<PromptHistoryRespDto> getPromptHistories(Long requestUserId) {
        User user = userMapper.selectById(requestUserId)
                .orElseThrow(() -> new BizException(PromptErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + requestUserId));

        // USER 권한: 같은 팀의 히스토리만, MANAGER/ADMIN: 전체
        // USER인데 팀 미배정(teamName == null)이면 빈 목록 반환
        String teamName = null;
        if ("USER".equals(user.getRole())) {
            if (user.getTeamName() == null) {
                log.warn("팀 미배정 USER의 히스토리 조회 요청 — userId={}", requestUserId);
                return List.of();
            }
            teamName = user.getTeamName();
        }

        List<PromptHistory> histories = promptHistoryMapper.selectList(teamName);

        return histories.stream()
                .map(history -> {
                    List<Long> tagIds = promptHistoryTagMapper.selectTagIdsByPromptHistoryId(history.getId());
                    return PromptHistoryRespDto.from(history, tagIds);
                })
                .toList();
    }
}

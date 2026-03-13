package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import com.nsmall.ns_creative_studio_bo.domain.category.mapper.CategoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.GoodsPromptHistoryTag;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptHistoryOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.GoodsPromptHistoryTagMapper;
import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Prompt 서비스 구현체
 *
 * <pre>
 * 프롬프트 생성 흐름:
 *   1. 사용자 존재 확인 + 일일 한도 체크
 *   2. 카테고리 존재 확인 + 태그 존재 확인
 *   3. Gemini API 호출 → 영문 프롬프트 생성
 *   4. PromptHistory + GoodsPromptHistoryTag 저장
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

    private static final int IMAGE_GENERATE_COUNT = 3;

    private final PromptHistoryMapper promptHistoryMapper;
    private final GoodsPromptHistoryTagMapper goodsPromptHistoryTagMapper;
    private final HumanPromptHistoryOptionMapper humanPromptHistoryOptionMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final GeminiService geminiService;
    private final ImageGenerationService imageGenerationService;
    private final ObjectMapper objectMapper;

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
        String promptText = geminiService.generateGoodsPrompt(goodsCategoryName, tagNames,
                dto.getPurpose(), dto.getAdditionalNotes());

        // 5. PromptHistory 저장
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();
        String promptStyle = (dto.getPromptStyle() != null && !dto.getPromptStyle().isBlank())
                ? dto.getPromptStyle() : "NANO_BANANA";

        PromptHistory history = PromptHistory.builder()
                .userId(currentUserId)
                .categoryId(dto.getGoodsCategoryId())
                .promptType("PRODUCT")
                .promptStyle(promptStyle)
                .promptText(promptText)
                .purpose(dto.getPurpose())
                .additionalNotes(dto.getAdditionalNotes())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        promptHistoryMapper.insert(history);

        // 6. GoodsPromptHistoryTag 저장 (태그가 있는 경우만)
        if (!dto.getTagIds().isEmpty()) {
            List<GoodsPromptHistoryTag> historyTags = dto.getTagIds().stream()
                    .map(tagId -> GoodsPromptHistoryTag.builder()
                            .promptHistoryId(history.getId())
                            .tagId(tagId)
                            .initRegiPic(requesterId)
                            .initRegiDttm(now)
                            .finalRvisPic(requesterId)
                            .finalRvisDttm(now)
                            .build())
                    .toList();

            goodsPromptHistoryTagMapper.insertBatch(historyTags);
        }

        // 7. 응답 반환
        String savedCategoryPath = String.join(" > ", categoryPath);
        List<Long> savedTagIds = goodsPromptHistoryTagMapper.selectTagIdsByPromptHistoryId(history.getId());
        List<String> savedTagNames = goodsPromptHistoryTagMapper.selectTagNamesByPromptHistoryId(history.getId());
        return PromptHistoryRespDto.fromGoods(history, savedCategoryPath, savedTagIds, savedTagNames);
    }

    @Override
    public List<PromptHistoryRespDto> getPromptHistories() {
        List<PromptHistory> histories = promptHistoryMapper.selectList();

        return histories.stream()
                .map(history -> {
                    if ("PRODUCT".equals(history.getPromptType())) {
                        String categoryPath = history.getCategoryId() != null
                                ? String.join(" > ", categoryMapper.selectCategoryPath(history.getCategoryId()))
                                : null;
                        List<Long> tagIds = goodsPromptHistoryTagMapper.selectTagIdsByPromptHistoryId(history.getId());
                        List<String> tagNames = goodsPromptHistoryTagMapper.selectTagNamesByPromptHistoryId(history.getId());
                        return PromptHistoryRespDto.fromGoods(history, categoryPath, tagIds, tagNames);
                    } else {
                        List<String> optionNames = humanPromptHistoryOptionMapper.selectOptionNamesByHistoryId(history.getId());
                        return PromptHistoryRespDto.fromHuman(history, optionNames);
                    }
                })
                .toList();
    }

    @Override
    @Transactional
    public List<String> generateGoodsImage(Long historyId, String promptText, MultipartFile referenceImage) {
        // 1. 이미지 IMAGE_GENERATE_COUNT개 병렬 생성 (참조 이미지 있으면 참조 이미지 기반, 없으면 텍스트만)
        List<String> imageUrls;
        if (referenceImage != null && !referenceImage.isEmpty()) {
            try {
                byte[] imageBytes = referenceImage.getBytes();
                String mimeType = referenceImage.getContentType();
                if (mimeType == null) {
                    throw new BizException(ImageGenerationErrorCode.INVALID_IMAGE_MIME_TYPE);
                }
                // 참조 이미지의 형태·비율·질감·색감을 보존하도록 지시문 앞에 추가
                // 조명·구도는 promptText에 위임하여 이미지 다양성을 확보
                String referenceGuidePrompt = "Use the provided reference image as the primary visual guide. "
                        + "Strictly preserve all of the following from the reference: "
                        + "(1) exact product shape, silhouette, and overall form without any distortion; "
                        + "(2) original proportions and scale of the product; "
                        + "(3) surface texture, material finish, and fine structural details; "
                        + "(4) color palette and tonal values of the product itself. "
                        + promptText;
                imageUrls = IntStream.range(0, IMAGE_GENERATE_COUNT)
                        .mapToObj(i -> CompletableFuture.supplyAsync(
                                () -> imageGenerationService.generateImageMime(referenceGuidePrompt, imageBytes, mimeType)))
                        .toList()
                        .stream()
                        .map(CompletableFuture::join)
                        .toList();
            } catch (IOException e) {
                log.error("참조 이미지 읽기 실패", e);
                throw new BizException(ImageGenerationErrorCode.IMAGE_SAVE_FAILED);
            }
        } else {
            imageUrls = imageGenerationService.generateImages(promptText, IMAGE_GENERATE_COUNT);
        }

        // 2. 이력에 JSON 배열로 저장 (image_url 컬럼에 "[url1,url2,url3]" 형태)
        if (historyId != null) {
            try {
                String imageUrlsJson = objectMapper.writeValueAsString(imageUrls);
                promptHistoryMapper.updateImageUrl(historyId, imageUrlsJson, SecurityUtil.getCurrentLoginId());
                log.info("상품 이미지 URL 이력 업데이트 완료 — historyId={}, urls={}", historyId, imageUrlsJson);
            } catch (JsonProcessingException e) {
                log.warn("이미지 URL JSON 직렬화 실패 — 첫 번째 URL만 저장. historyId={}", historyId, e);
                promptHistoryMapper.updateImageUrl(historyId, imageUrls.get(0), SecurityUtil.getCurrentLoginId());
            }
        }

        return imageUrls;
    }
}

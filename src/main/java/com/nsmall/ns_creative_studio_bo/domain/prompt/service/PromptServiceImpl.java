package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.page.PageHelperUtil;
import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptType;
import com.nsmall.ns_creative_studio_bo.domain.category.mapper.CategoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryPathDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptHistorySearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.GoodsTagDetailDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanOptionDetailDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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

    private static final long MAX_REFERENCE_IMAGE_BYTES = 15 * 1024 * 1024L; // 15MB
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final PromptHistoryMapper promptHistoryMapper;
    private final GoodsPromptHistoryTagMapper goodsPromptHistoryTagMapper;
    private final HumanPromptHistoryOptionMapper humanPromptHistoryOptionMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final GeminiService geminiService;
    private final ImageGenerationService imageGenerationService;
    private final ObjectMapper objectMapper;

    @Value("${image.base-url}")
    private String imageBaseUrl;

    @Value("${image.storage-path}")
    private String storagePath;

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

        // 2. 상품 카테고리 조회 + SCS 검증
        Category category = categoryMapper.selectById(dto.getGoodsCategoryId())
                .orElseThrow(() -> new BizException(PromptErrorCode.CATEGORY_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다. categoryId=" + dto.getGoodsCategoryId()));

        if (CategoryType.SCS != category.getCategoryType()) {
            throw new BizException(PromptErrorCode.INVALID_CATEGORY_TYPE,
                    "SCS 카테고리가 아닙니다. categoryType=" + category.getCategoryType());
        }

        // SCS 이름만 보내면 맥락이 불분명하므로 LCS→SCS 전체 경로를 조합하여 전달
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
        PromptStyle promptStyle = dto.getPromptStyle() != null ? dto.getPromptStyle() : PromptStyle.NANO_BANANA;
        String promptText = geminiService.generateGoodsPrompt(goodsCategoryName, tagNames,
                dto.getImageAspectRatio(), dto.getPurpose(), dto.getAdditionalNotes(), promptStyle);

        // 5. PromptHistory 저장
        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        PromptHistory history = PromptHistory.builder()
                .userId(currentUserId)
                .categoryId(dto.getGoodsCategoryId())
                .promptType(PromptType.PRODUCT.name())
                .promptStyle(promptStyle.name())
                .promptText(promptText)
                .imageAspectRatio(dto.getImageAspectRatio())
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
        return PromptHistoryRespDto.fromGoods(history, savedCategoryPath, savedTagIds, savedTagNames, imageBaseUrl);
    }

    @Override
    public PageRespDto<PromptHistoryRespDto> getPromptHistories(PromptHistorySearchReqDto searchReqDto) {
        // 쿼리 1: 페이지 조회
        PageInfo<PromptHistory> pageInfo = PageHelperUtil.pagedQuery(
                searchReqDto, () -> promptHistoryMapper.selectList(searchReqDto));

        List<PromptHistory> list = pageInfo.getList();

        List<Long> productHistoryIds = list.stream()
                .filter(h -> PromptType.PRODUCT.name().equals(h.getPromptType()))
                .map(PromptHistory::getId)
                .toList();

        List<Long> humanHistoryIds = list.stream()
                .filter(h -> PromptType.PERSON.name().equals(h.getPromptType()))
                .map(PromptHistory::getId)
                .toList();

        // 쿼리 2: PRODUCT — 카테고리 경로 배치 조회
        // 쿼리 3: PRODUCT — 태그 상세(tagId + tagName) 배치 조회
        Map<Long, String> categoryPathMap = Map.of();
        Map<Long, List<Long>> tagIdsMap = Map.of();
        Map<Long, List<String>> tagNamesMap = Map.of();

        if (!productHistoryIds.isEmpty()) {
            List<Long> categoryIds = list.stream()
                    .filter(h -> PromptType.PRODUCT.name().equals(h.getPromptType()) && h.getCategoryId() != null)
                    .map(PromptHistory::getCategoryId)
                    .distinct()
                    .toList();

            categoryPathMap = categoryMapper.selectCategoryPathsByIds(categoryIds)
                    .stream()
                    .collect(Collectors.toMap(CategoryPathDto::getId, CategoryPathDto::getCategoryPath));

            List<GoodsTagDetailDto> tagDetails =
                    goodsPromptHistoryTagMapper.selectTagDetailsByPromptHistoryIds(productHistoryIds);

            tagIdsMap = tagDetails.stream()
                    .collect(Collectors.groupingBy(
                            GoodsTagDetailDto::getPromptHistoryId,
                            Collectors.mapping(GoodsTagDetailDto::getTagId, Collectors.toList())));

            tagNamesMap = tagDetails.stream()
                    .collect(Collectors.groupingBy(
                            GoodsTagDetailDto::getPromptHistoryId,
                            Collectors.mapping(GoodsTagDetailDto::getTagName, Collectors.toList())));
        }

        // 쿼리 4: HUMAN — 옵션명 배치 조회
        Map<Long, List<String>> optionNamesMap = Map.of();
        if (!humanHistoryIds.isEmpty()) {
            optionNamesMap = humanPromptHistoryOptionMapper
                    .selectOptionDetailsByHistoryIds(humanHistoryIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            HumanOptionDetailDto::getPromptHistoryId,
                            Collectors.mapping(HumanOptionDetailDto::getOptionName, Collectors.toList())));
        }

        // Java에서 조립 (추가 쿼리 없음)
        final Map<Long, String> finalCategoryPathMap = categoryPathMap;
        final Map<Long, List<Long>> finalTagIdsMap = tagIdsMap;
        final Map<Long, List<String>> finalTagNamesMap = tagNamesMap;
        final Map<Long, List<String>> finalOptionNamesMap = optionNamesMap;

        List<PromptHistoryRespDto> items = list.stream()
                .map(history -> {
                    if (PromptType.PRODUCT.name().equals(history.getPromptType())) {
                        String categoryPath = history.getCategoryId() != null
                                ? finalCategoryPathMap.get(history.getCategoryId())
                                : null;
                        List<Long> tagIds = finalTagIdsMap.getOrDefault(history.getId(), List.of());
                        List<String> tagNames = finalTagNamesMap.getOrDefault(history.getId(), List.of());
                        return PromptHistoryRespDto.fromGoods(history, categoryPath, tagIds, tagNames, imageBaseUrl);
                    } else {
                        List<String> optionNames = finalOptionNamesMap.getOrDefault(history.getId(), List.of());
                        return PromptHistoryRespDto.fromHuman(history, optionNames, imageBaseUrl);
                    }
                })
                .toList();

        return PageRespDto.ofWithItems(pageInfo, items);
    }

    @Override
    @Transactional
    public List<String> generateGoodsImage(Long historyId, String promptText, MultipartFile referenceImage) {
        // 1. 이미지 PromptConstants.IMAGE_GENERATE_COUNT개 병렬 생성 (참조 이미지 있으면 참조 이미지 기반, 없으면 텍스트만)
        List<String> imageUrls;
        if (referenceImage != null && !referenceImage.isEmpty()) {
            try {
                if (referenceImage.getSize() > MAX_REFERENCE_IMAGE_BYTES) {
                    throw new BizException(ImageGenerationErrorCode.IMAGE_TOO_LARGE);
                }
                String mimeType = referenceImage.getContentType();
                if (mimeType == null) {
                    throw new BizException(ImageGenerationErrorCode.INVALID_IMAGE_MIME_TYPE);
                }
                if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)) {
                    log.warn("허용되지 않은 MIME 타입 — mimeType={}", mimeType);
                    throw new BizException(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE);
                }
                byte[] imageBytes = referenceImage.getBytes();
                // 참조 이미지의 형태·비율·질감·색감을 보존하도록 지시문 앞에 추가
                // 조명·구도는 promptText에 위임하여 이미지 다양성을 확보
                String referenceGuidePrompt = "Use the provided reference image as the primary visual guide. "
                        + "Strictly preserve all of the following from the reference: "
                        + "(1) exact product shape, silhouette, and overall form without any distortion; "
                        + "(2) original proportions and scale of the product; "
                        + "(3) surface texture, material finish, and fine structural details; "
                        + "(4) color palette and tonal values of the product itself. "
                        + promptText;
                imageUrls = IntStream.range(0, PromptConstants.IMAGE_GENERATE_COUNT)
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
            imageUrls = imageGenerationService.generateImages(promptText, PromptConstants.IMAGE_GENERATE_COUNT);
        }

        // 2. 이력에 상대 경로 JSON 배열로 저장 (image_url 컬럼에 "[/images/uuid.jpg, ...]" 형태)
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

        // 3. 응답에는 절대 URL로 조립하여 반환
        return imageUrls.stream()
                .map(path -> imageBaseUrl + path)
                .toList();
    }

    @Override
    @Transactional
    public void deleteHistory(Long id) {
        PromptHistory history = promptHistoryMapper.selectById(id)
                .orElseThrow(() -> new BizException(PromptErrorCode.HISTORY_NOT_FOUND,
                        "프롬프트 이력을 찾을 수 없습니다. id=" + id));

        // 1. 자식 레코드 먼저 삭제 (매핑 테이블)
        if (PromptType.PRODUCT.name().equals(history.getPromptType())) {
            goodsPromptHistoryTagMapper.deleteByPromptHistoryId(id);
        } else {
            humanPromptHistoryOptionMapper.deleteByPromptHistoryId(id);
        }

        // 2. 부모 레코드 삭제
        promptHistoryMapper.deleteById(id);

        // 3. 디스크 이미지 파일 삭제 (롤백 불가 — DB 작업과 동일 트랜잭션 내 실행)
        deleteImageFiles(history.getImageUrl());
    }

    /**
     * imageUrl 컬럼값을 파싱하여 디스크에 저장된 이미지 파일을 삭제한다.
     *
     * <pre>
     * - null / blank        → 스킵
     * - "[...]" (JSON 배열) → 다수 파일 삭제 (NanoBanana 3개)
     * - 단일 상대 경로      → 파일 1개 삭제
     * </pre>
     */
    private void deleteImageFiles(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        List<String> relativePaths = new ArrayList<>();
        if (imageUrl.startsWith("[")) {
            try {
                relativePaths = objectMapper.readValue(imageUrl,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                log.warn("이미지 URL JSON 파싱 실패 — 단일 경로로 처리. imageUrl={}", imageUrl, e);
                relativePaths.add(imageUrl);
            }
        } else {
            relativePaths.add(imageUrl);
        }

        for (String relativePath : relativePaths) {
            // "/images/uuid.jpg" → storagePath + "/uuid.jpg"
            String filename = Paths.get(relativePath).getFileName().toString();
            Path filePath = Paths.get(storagePath, filename);
            try {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info("이미지 파일 삭제 완료 — path={}", filePath);
                } else {
                    log.warn("이미지 파일이 존재하지 않습니다 — path={}", filePath);
                }
            } catch (IOException e) {
                log.error("이미지 파일 삭제 실패 — path={}", filePath, e);
            }
        }
    }
}

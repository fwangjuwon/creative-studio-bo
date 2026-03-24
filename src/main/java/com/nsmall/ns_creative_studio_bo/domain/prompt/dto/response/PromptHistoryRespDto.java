package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프롬프트 히스토리 응답 DTO
 *
 * <pre>
 * promptType에 따라 유효한 필드가 다르다.
 *
 * PRODUCT: categoryId, categoryPath, tagIds, tagNames 사용
 *          purpose, additionalNotes, optionNames 는 null
 *
 * PERSON : purpose, additionalNotes, optionNames 사용
 *          categoryId, categoryPath, tagIds, tagNames 는 null
 *
 * imageUrl  : DB에 저장된 원본값 (단일 URL 또는 JSON 배열 문자열)
 * imageUrls : imageUrl을 파싱한 이미지 URL 목록 (항상 List 형태)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistoryRespDto {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private Long userId;

    // 공통
    private String promptType;        // 'PRODUCT' | 'PERSON'
    private String promptStyle;       // 'NANO_BANANA' | 'MID_JOURNEY'
    private String promptText;
    private String imageAspectRatio;  // 선택한 이미지 비율 (예: "4:3", "1:1", nullable)
    private String imageUrl;          // DB 원본값 (단일 URL 또는 JSON 배열 문자열)
    private List<String> imageUrls;   // 파싱된 이미지 URL 목록

    // 상품 프롬프트 전용
    private Long categoryId;
    private String categoryPath;     // "식품 > 건강식품 > 인삼"
    private List<Long> tagIds;
    private List<String> tagNames;

    // 인물 프롬프트 전용
    private String purpose;
    private String additionalNotes;
    private List<String> optionNames;  // 선택한 옵션 한국어명 목록

    // 감사 컬럼
    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    /**
     * image_url 컬럼값(단일 URL 또는 JSON 배열 문자열)을 List로 파싱한다.
     *
     * <pre>
     * - null / blank        → 빈 List
     * - "[...]" (JSON 배열) → Jackson 파싱
     * - 일반 URL 문자열     → 단일 원소 List
     * </pre>
     */
    private static List<String> parseImageUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        if (raw.startsWith("[")) {
            try {
                return OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return List.of(raw);
            }
        }
        return List.of(raw);
    }

    /**
     * 상품 프롬프트 이력 응답 생성.
     *
     * @param categoryPath "식품 > 건강식품 > 인삼" 형태의 카테고리 전체 경로
     * @param tagNames     선택한 태그 한국어명 목록
     */
    public static PromptHistoryRespDto fromGoods(PromptHistory history,
                                                 String categoryPath,
                                                 List<Long> tagIds,
                                                 List<String> tagNames) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .promptType(history.getPromptType())
                .promptStyle(history.getPromptStyle())
                .promptText(history.getPromptText())
                .imageAspectRatio(history.getImageAspectRatio())
                .imageUrl(history.getImageUrl())
                .imageUrls(parseImageUrls(history.getImageUrl()))
                .categoryId(history.getCategoryId())
                .categoryPath(categoryPath)
                .tagIds(tagIds)
                .tagNames(tagNames)
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }

    /**
     * 인물 프롬프트 이력 응답 생성.
     *
     * @param optionNames 선택한 옵션 한국어명 목록 (단계 순서대로 정렬)
     */
    public static PromptHistoryRespDto fromHuman(PromptHistory history, List<String> optionNames) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .promptType(history.getPromptType())
                .promptStyle(history.getPromptStyle())
                .promptText(history.getPromptText())
                .imageAspectRatio(history.getImageAspectRatio())
                .imageUrl(history.getImageUrl())
                .imageUrls(parseImageUrls(history.getImageUrl()))
                .purpose(history.getPurpose())
                .additionalNotes(history.getAdditionalNotes())
                .optionNames(optionNames)
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }
}

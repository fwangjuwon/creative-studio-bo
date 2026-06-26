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
 * PRODUCT  : categoryId, categoryPath, tagIds, tagNames, purpose, additionalNotes 사용
 *            personOptions 는 null
 *
 * PERSON   : purpose, additionalNotes, personOptions 사용
 *            categoryId, categoryPath, tagIds, tagNames 는 null
 *            personOptions: personIndex -1(공통), 0 이상(인물 인덱스) 로 그룹핑
 *
 * FUSION   : imageAspectRatio, textContent, disclaimerText 사용
 *            personOptions, categoryId, categoryPath, tagIds, tagNames 는 null
 *
 * TEXT_OVL : textContent, disclaimerText 사용
 *            imageAspectRatio, personOptions, categoryId, categoryPath, tagIds, tagNames 는 null
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
    /** 인물별 선택 옵션 그룹 목록 (personIndex -1=공통, 0~N=인물 인덱스) */
    private List<PersonOptionGroupDto> personOptions;

    // 텍스트 오버레이 (FUSION·TEXT_OVL 공통, 나머지는 null)
    private String textContent;
    private String disclaimerText;

    /**
     * 인물별 선택 옵션 그룹 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonOptionGroupDto {
        /** -1: 공통(조명/촬영), 0 이상: 인물 인덱스 */
        private int personIndex;
        /** "공통", "사람 1", "사람 2" ... */
        private String label;
        private List<String> optionNames;
    }

    // 감사 컬럼
    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    /**
     * image_url 컬럼값(단일 URL 또는 JSON 배열 문자열)을 파싱하고 baseUrl을 prefix로 붙여 반환한다.
     *
     * <pre>
     * - null / blank        → 빈 List
     * - "[...]" (JSON 배열) → Jackson 파싱 후 각 URL에 baseUrl prefix
     * - 일반 URL 문자열     → 단일 원소 List (baseUrl prefix)
     * </pre>
     */
    private static List<String> parseImageUrls(String raw, String baseUrl) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> paths;
        if (raw.startsWith("[")) {
            try {
                paths = OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                paths = List.of(raw);
            }
        } else {
            paths = List.of(raw);
        }
        return paths.stream()
                .map(path -> path.startsWith("http") ? path : baseUrl + path)
                .toList();
    }

    /**
     * 상품 프롬프트 이력 응답 생성.
     *
     * @param categoryPath  "식품 > 건강식품 > 인삼" 형태의 카테고리 전체 경로
     * @param tagNames      선택한 태그 한국어명 목록
     * @param imageBaseUrl  이미지 서빙 기준 URL (예: https://bo.nsmall.com)
     *                      purpose, additionalNotes, textContent 는 history 에서 직접 매핑
     */
    public static PromptHistoryRespDto fromGoods(PromptHistory history,
                                                 String categoryPath,
                                                 List<Long> tagIds,
                                                 List<String> tagNames,
                                                 String imageBaseUrl) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .promptType(history.getPromptType())
                .promptStyle(history.getPromptStyle())
                .promptText(history.getPromptText())
                .imageAspectRatio(history.getImageAspectRatio())
                .imageUrl(history.getImageUrl())
                .imageUrls(parseImageUrls(history.getImageUrl(), imageBaseUrl))
                .categoryId(history.getCategoryId())
                .categoryPath(categoryPath)
                .tagIds(tagIds)
                .tagNames(tagNames)
                .purpose(history.getPurpose())
                .additionalNotes(history.getAdditionalNotes())
                .textContent(history.getTextContent())
                .disclaimerText(history.getDisclaimerText())
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }

    /**
     * 인물 프롬프트 이력 응답 생성.
     *
     * @param personOptions 인물별 선택 옵션 그룹 목록
     * @param imageBaseUrl  이미지 서빙 기준 URL (예: https://bo.nsmall.com)
     */
    public static PromptHistoryRespDto fromHuman(PromptHistory history,
                                                 List<PersonOptionGroupDto> personOptions,
                                                 String imageBaseUrl) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .promptType(history.getPromptType())
                .promptStyle(history.getPromptStyle())
                .promptText(history.getPromptText())
                .imageAspectRatio(history.getImageAspectRatio())
                .imageUrl(history.getImageUrl())
                .imageUrls(parseImageUrls(history.getImageUrl(), imageBaseUrl))
                .purpose(history.getPurpose())
                .additionalNotes(history.getAdditionalNotes())
                .personOptions(personOptions)
                .textContent(history.getTextContent())
                .disclaimerText(history.getDisclaimerText())
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }

    /**
     * 합성·텍스트합성(FUSION·TEXT_OVL) 프롬프트 이력 응답 생성.
     * personOptions, categoryId, categoryPath, tagIds, tagNames 는 null.
     *
     * @param imageBaseUrl 이미지 서빙 기준 URL (예: https://bo.nsmall.com)
     */
    public static PromptHistoryRespDto fromFusion(PromptHistory history, String imageBaseUrl) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .promptType(history.getPromptType())
                .promptStyle(history.getPromptStyle())
                .promptText(history.getPromptText())
                .imageAspectRatio(history.getImageAspectRatio())
                .imageUrl(history.getImageUrl())
                .imageUrls(parseImageUrls(history.getImageUrl(), imageBaseUrl))
                .textContent(history.getTextContent())
                .disclaimerText(history.getDisclaimerText())
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }
}

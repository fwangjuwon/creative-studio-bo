package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

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

    private Long id;
    private Long userId;

    // 공통
    private String promptType;       // 'PRODUCT' | 'PERSON'
    private String promptStyle;      // 'NANO_BANANA' | 'MID_JOURNEY'
    private String promptText;
    private String imageUrl;

    // 상품 프롬프트 전용
    private Long categoryId;
    private String categoryPath;     // "식품 > 건강식품 > 약재 > 인삼"
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
     * 상품 프롬프트 이력 응답 생성.
     *
     * @param categoryPath "식품 > 건강식품 > 약재 > 인삼" 형태의 카테고리 전체 경로
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
                .imageUrl(history.getImageUrl())
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
                .imageUrl(history.getImageUrl())
                .purpose(history.getPurpose())
                .additionalNotes(history.getAdditionalNotes())
                .optionNames(optionNames)
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }
}

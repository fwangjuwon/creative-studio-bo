package com.nsmall.ns_creative_studio_bo.domain.category.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Category 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRespDto {

    private Long id;
    private String name;
    private CategoryType categoryType;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    public static CategoryRespDto from(Category category) {
        return CategoryRespDto.builder()
                .id(category.getId())
                .name(category.getName())
                .categoryType(category.getCategoryType())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .initRegiDttm(category.getInitRegiDttm())
                .finalRvisDttm(category.getFinalRvisDttm())
                .build();
    }
}

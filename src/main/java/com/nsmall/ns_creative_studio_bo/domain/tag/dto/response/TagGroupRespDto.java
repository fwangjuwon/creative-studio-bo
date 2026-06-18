package com.nsmall.ns_creative_studio_bo.domain.tag.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.tag.entity.TagGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TagGroup 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagGroupRespDto {

    private Long id;
    private String name;
    private Boolean isMultiSelect;
    private Integer sortOrder;
    private String isFeatured;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    public static TagGroupRespDto from(TagGroup tagGroup) {
        return TagGroupRespDto.builder()
                .id(tagGroup.getId())
                .name(tagGroup.getName())
                .isMultiSelect(tagGroup.getIsMultiSelect())
                .sortOrder(tagGroup.getSortOrder())
                .isFeatured(tagGroup.getIsFeatured())
                .initRegiDttm(tagGroup.getInitRegiDttm())
                .finalRvisDttm(tagGroup.getFinalRvisDttm())
                .build();
    }
}

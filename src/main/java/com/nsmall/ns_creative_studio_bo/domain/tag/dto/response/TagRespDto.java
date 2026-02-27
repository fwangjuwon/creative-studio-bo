package com.nsmall.ns_creative_studio_bo.domain.tag.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tag 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRespDto {

    private Long id;
    private Long tagGroupId;
    private String name;
    private Integer sortOrder;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    public static TagRespDto from(Tag tag) {
        return TagRespDto.builder()
                .id(tag.getId())
                .tagGroupId(tag.getTagGroupId())
                .name(tag.getName())
                .sortOrder(tag.getSortOrder())
                .initRegiDttm(tag.getInitRegiDttm())
                .finalRvisDttm(tag.getFinalRvisDttm())
                .build();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.tag.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tag 수정 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagUpdateReqDto {

    private String name;
    private Integer sortOrder;
}

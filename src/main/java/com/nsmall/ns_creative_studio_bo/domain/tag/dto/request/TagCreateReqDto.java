package com.nsmall.ns_creative_studio_bo.domain.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tag 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagCreateReqDto {

    @NotNull(message = "태그 그룹 ID는 필수입니다.")
    private Long tagGroupId;

    @NotBlank(message = "태그명은 필수입니다.")
    private String name;

    private Integer sortOrder;
}

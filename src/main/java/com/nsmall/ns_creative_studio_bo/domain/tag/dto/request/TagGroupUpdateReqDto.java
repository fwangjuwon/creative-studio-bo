package com.nsmall.ns_creative_studio_bo.domain.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TagGroup 수정 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagGroupUpdateReqDto {

    @NotBlank(message = "태그 그룹명은 필수입니다.")
    @Size(max = 100, message = "태그 그룹명은 100자 이하여야 합니다.")
    private String name;

    @NotNull(message = "복수 선택 허용 여부는 필수입니다.")
    private Boolean isMultiSelect;

    private Integer sortOrder;

    /** 태그 그룹 유형 (선택). 'PROMPT' | 'TEXT_OVERLAY' */
    private String type;
}

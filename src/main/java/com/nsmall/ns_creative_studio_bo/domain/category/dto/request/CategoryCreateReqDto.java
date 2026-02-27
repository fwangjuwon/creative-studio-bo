package com.nsmall.ns_creative_studio_bo.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Category 생성 요청 DTO
 *
 * <pre>
 * parentId — LCS 생성 시 null, MCS/SCS/DCS 생성 시 상위 카테고리 id
 * sortOrder — nullable, 미입력 시 null 저장
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCreateReqDto {

    @NotBlank(message = "카테고리명은 필수입니다.")
    private String name;

    @NotBlank(message = "카테고리 타입은 필수입니다.")
    @Pattern(regexp = "^(LCS|MCS|SCS|DCS)$", message = "카테고리 타입은 LCS, MCS, SCS, DCS 중 하나여야 합니다.")
    private String categoryType;

    private Long parentId;

    private Integer sortOrder;
}

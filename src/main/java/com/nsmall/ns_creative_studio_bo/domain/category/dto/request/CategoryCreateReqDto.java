package com.nsmall.ns_creative_studio_bo.domain.category.dto.request;

import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Category 생성 요청 DTO
 *
 * <pre>
 * parentId — LCS 생성 시 null, MCS/SCS 생성 시 상위 카테고리 id
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
    @Size(max = 200, message = "카테고리명은 200자 이하여야 합니다.")
    private String name;

    @NotNull(message = "카테고리 타입은 필수입니다.")
    private CategoryType categoryType;

    private Long parentId;

    private Integer sortOrder;
}

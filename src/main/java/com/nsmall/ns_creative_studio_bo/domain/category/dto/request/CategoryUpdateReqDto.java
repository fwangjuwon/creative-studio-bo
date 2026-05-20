package com.nsmall.ns_creative_studio_bo.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Category 수정 요청 DTO
 *
 * <pre>
 * name, sortOrder만 수정 가능.
 * categoryType, parentId는 생성 후 변경 불가 (계층 구조 일관성 유지).
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateReqDto {

    @NotBlank(message = "카테고리명은 필수입니다.")
    @Size(max = 200, message = "카테고리명은 200자 이하여야 합니다.")
    private String name;

    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;
}

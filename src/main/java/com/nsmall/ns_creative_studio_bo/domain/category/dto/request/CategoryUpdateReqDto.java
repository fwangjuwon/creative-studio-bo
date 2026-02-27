package com.nsmall.ns_creative_studio_bo.domain.category.dto.request;

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

    private String name;

    private Integer sortOrder;
}

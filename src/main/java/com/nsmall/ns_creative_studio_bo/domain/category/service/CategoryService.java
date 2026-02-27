package com.nsmall.ns_creative_studio_bo.domain.category.service;

import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;

import java.util.List;
import java.util.Optional;

/**
 * Category 서비스 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface CategoryService {

    CategoryRespDto createCategory(CategoryCreateReqDto dto);

    /**
     * 단건 조회 — 결과 없으면 Optional.empty() 반환 (컨트롤러에서 200 + data:null 처리)
     */
    Optional<CategoryRespDto> getCategory(Long id);

    /**
     * 카테고리 목록 조회 — 단계별 드릴다운
     *
     * <pre>
     * parentId == null → LCS(대분류) 목록
     * parentId != null → 해당 parent의 하위 카테고리 목록
     * </pre>
     */
    List<CategoryRespDto> getCategories(Long parentId);

    CategoryRespDto updateCategory(Long id, CategoryUpdateReqDto dto);

    void deleteCategory(Long id);
}

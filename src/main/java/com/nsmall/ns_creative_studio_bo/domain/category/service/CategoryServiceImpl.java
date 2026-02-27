package com.nsmall.ns_creative_studio_bo.domain.category.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import com.nsmall.ns_creative_studio_bo.domain.category.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Category 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public CategoryRespDto createCategory(CategoryCreateReqDto dto) {
        boolean isLcs = "LCS".equals(dto.getCategoryType());

        if (isLcs && dto.getParentId() != null) {
            throw new BizException(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY,
                    "LCS(대분류)는 상위 카테고리를 가질 수 없습니다.");
        }
        if (!isLcs && dto.getParentId() == null) {
            throw new BizException(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY,
                    dto.getCategoryType() + "는 상위 카테고리가 필요합니다.");
        }

        if (dto.getParentId() != null) {
            categoryMapper.selectById(dto.getParentId())
                    .orElseThrow(() -> new BizException(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND,
                            "상위 카테고리를 찾을 수 없습니다. parentId=" + dto.getParentId()));
        }

        LocalDateTime now = LocalDateTime.now();
        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        Category category = Category.builder()
                .name(dto.getName())
                .categoryType(dto.getCategoryType())
                .parentId(dto.getParentId())
                .sortOrder(dto.getSortOrder())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        categoryMapper.insert(category);

        return categoryMapper.selectById(category.getId())
                .map(CategoryRespDto::from)
                .orElse(null);
    }

    @Override
    public Optional<CategoryRespDto> getCategory(Long id) {
        return categoryMapper.selectById(id).map(CategoryRespDto::from);
    }

    @Override
    public List<CategoryRespDto> getCategories(Long parentId) {
        return categoryMapper.selectList(parentId).stream()
                .map(CategoryRespDto::from)
                .toList();
    }

    @Override
    public CategoryRespDto updateCategory(Long id, CategoryUpdateReqDto dto) {
        categoryMapper.selectById(id)
                .orElseThrow(() -> new BizException(CategoryErrorCode.CATEGORY_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다. id=" + id));

        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        Category updated = Category.builder()
                .id(id)
                .name(dto.getName())
                .sortOrder(dto.getSortOrder())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        categoryMapper.update(updated);

        return categoryMapper.selectById(id)
                .map(CategoryRespDto::from)
                .orElse(null);
    }

    @Override
    public void deleteCategory(Long id) {
        categoryMapper.selectById(id)
                .orElseThrow(() -> new BizException(CategoryErrorCode.CATEGORY_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다. id=" + id));

        categoryMapper.deleteById(id);
    }
}

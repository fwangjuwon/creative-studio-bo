package com.nsmall.ns_creative_studio_bo.domain.category.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import com.nsmall.ns_creative_studio_bo.domain.category.mapper.CategoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    /** 하위 타입 → 기대되는 부모 타입 매핑 (MCS 부모=LCS, SCS 부모=MCS) */
    private static final Map<CategoryType, CategoryType> EXPECTED_PARENT_TYPE = Map.of(
            CategoryType.MCS, CategoryType.LCS,
            CategoryType.SCS, CategoryType.MCS
    );

    private final CategoryMapper categoryMapper;
    private final PromptHistoryMapper promptHistoryMapper;

    @Override
    public CategoryRespDto createCategory(CategoryCreateReqDto dto) {
        boolean isLcs = CategoryType.LCS == dto.getCategoryType();

        if (isLcs && dto.getParentId() != null) {
            throw new BizException(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY,
                    "LCS(대분류)는 상위 카테고리를 가질 수 없습니다.");
        }
        if (!isLcs && dto.getParentId() == null) {
            throw new BizException(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY,
                    dto.getCategoryType() + "는 상위 카테고리가 필요합니다.");
        }

        if (dto.getParentId() != null) {
            Category parent = categoryMapper.selectById(dto.getParentId())
                    .orElseThrow(() -> new BizException(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND,
                            "상위 카테고리를 찾을 수 없습니다. parentId=" + dto.getParentId()));

            CategoryType expectedParent = EXPECTED_PARENT_TYPE.get(dto.getCategoryType());
            if (expectedParent != parent.getCategoryType()) {
                throw new BizException(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY,
                        dto.getCategoryType() + "의 상위 카테고리는 " + expectedParent
                        + "이어야 합니다. 실제 타입: " + parent.getCategoryType());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

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

        String requesterId = SecurityUtil.getCurrentLoginId();

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

        if (categoryMapper.existsByParentId(id)) {
            throw new BizException(CategoryErrorCode.CATEGORY_HAS_CHILDREN,
                    "하위 카테고리가 존재하여 삭제할 수 없습니다. id=" + id);
        }

        if (promptHistoryMapper.existsByCategoryId(id)) {
            throw new BizException(CategoryErrorCode.CATEGORY_IN_USE,
                    "프롬프트 히스토리에서 사용 중인 카테고리는 삭제할 수 없습니다. id=" + id);
        }

        categoryMapper.deleteById(id);
    }
}

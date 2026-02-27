package com.nsmall.ns_creative_studio_bo.domain.category.mapper;

import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * Category MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface CategoryMapper {

    void insert(Category category);

    Optional<Category> selectById(Long id);

    /**
     * 카테고리 목록 조회 — 단계별 드릴다운
     *
     * <pre>
     * parentId == null → LCS(대분류) 목록 (WHERE parent_id IS NULL)
     * parentId != null → 해당 parent의 하위 카테고리 목록 (WHERE parent_id = #{parentId})
     * </pre>
     */
    List<Category> selectList(Long parentId);

    void update(Category category);

    void deleteById(Long id);
}

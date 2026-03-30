package com.nsmall.ns_creative_studio_bo.domain.category.mapper;

import com.nsmall.ns_creative_studio_bo.domain.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    Optional<Category> selectById(@Param("id") Long id);

    /**
     * 카테고리 목록 조회 — 단계별 드릴다운
     *
     * <pre>
     * parentId == null → LCS(대분류) 목록 (WHERE parent_id IS NULL)
     * parentId != null → 해당 parent의 하위 카테고리 목록 (WHERE parent_id = #{parentId})
     * </pre>
     */
    List<Category> selectList(@Param("parentId") Long parentId);

    boolean existsByParentId(@Param("parentId") Long parentId);

    /**
     * 동일 parent_id 스코프에서 sortOrder 중복 여부 확인
     *
     * <pre>
     * parentId == null → LCS 스코프 (WHERE parent_id IS NULL)
     * parentId != null → 해당 parent의 하위 스코프 (WHERE parent_id = #{parentId})
     * </pre>
     */
    boolean existsBySortOrder(@Param("parentId") Long parentId, @Param("sortOrder") Integer sortOrder);

    /**
     * 동일 parent_id 스코프에서 자신(id)을 제외한 sortOrder 중복 여부 확인 — 수정 시 사용
     */
    boolean existsBySortOrderExcludingId(@Param("id") Long id, @Param("parentId") Long parentId, @Param("sortOrder") Integer sortOrder);

    /**
     * SCS 카테고리 ID로 LCS→MCS→SCS 전체 경로 이름 목록을 반환한다.
     *
     * <pre>
     * PostgreSQL 재귀 CTE 사용.
     * 반환 순서: LCS 이름, MCS 이름, SCS 이름
     * </pre>
     */
    List<String> selectCategoryPath(@Param("id") Long id);

    void update(Category category);

    void deleteById(@Param("id") Long id);
}

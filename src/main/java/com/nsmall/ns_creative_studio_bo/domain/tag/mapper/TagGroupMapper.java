package com.nsmall.ns_creative_studio_bo.domain.tag.mapper;

import com.nsmall.ns_creative_studio_bo.domain.tag.entity.TagGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * TagGroup MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface TagGroupMapper {

    void insert(TagGroup tagGroup);

    Optional<TagGroup> selectById(@Param("id") Long id);

    List<TagGroup> selectList();

    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    void update(TagGroup tagGroup);

    void deleteById(@Param("id") Long id);

    /** 전체 태그 그룹 중 최대 sort_order 조회 — 자동 증가 시 사용 (데이터 없으면 0 반환) */
    int selectMaxSortOrder();

    /** 전체 스코프에서 sortOrder 중복 여부 확인 */
    boolean existsBySortOrder(@Param("sortOrder") Integer sortOrder);

    /** 전체 스코프에서 자신(id)을 제외한 sortOrder 중복 여부 확인 — 수정 시 사용 */
    boolean existsBySortOrderExcludingId(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}

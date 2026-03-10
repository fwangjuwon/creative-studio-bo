package com.nsmall.ns_creative_studio_bo.domain.tag.mapper;

import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * Tag MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface TagMapper {

    void insert(Tag tag);

    Optional<Tag> selectById(@Param("id") Long id);

    /**
     * TagGroup별 태그 목록 조회
     *
     * <pre>
     * tagGroupId != null → 해당 그룹의 태그 목록
     * tagGroupId == null → 전체 태그 목록
     * </pre>
     */
    List<Tag> selectList(@Param("tagGroupId") Long tagGroupId);

    boolean existsByTagGroupId(@Param("tagGroupId") Long tagGroupId);

    void update(Tag tag);

    void deleteById(@Param("id") Long id);
}

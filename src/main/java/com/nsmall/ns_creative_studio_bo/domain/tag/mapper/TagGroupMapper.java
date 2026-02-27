package com.nsmall.ns_creative_studio_bo.domain.tag.mapper;

import com.nsmall.ns_creative_studio_bo.domain.tag.entity.TagGroup;
import org.apache.ibatis.annotations.Mapper;

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

    Optional<TagGroup> selectById(Long id);

    List<TagGroup> selectAll();

    void update(TagGroup tagGroup);

    void deleteById(Long id);
}

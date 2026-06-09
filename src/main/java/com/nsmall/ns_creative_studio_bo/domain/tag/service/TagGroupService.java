package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;

import java.util.List;
import java.util.Optional;

/**
 * TagGroup 서비스 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface TagGroupService {

    TagGroupRespDto createTagGroup(TagGroupCreateReqDto dto);

    Optional<TagGroupRespDto> getTagGroup(Long id);

    List<TagGroupRespDto> getTagGroups();

    TagGroupRespDto updateTagGroup(Long id, TagGroupUpdateReqDto dto);

    void deleteTagGroup(Long id);
}

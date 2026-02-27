package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;

import java.util.List;
import java.util.Optional;

/**
 * Tag 서비스 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface TagService {

    TagRespDto createTag(TagCreateReqDto dto);

    Optional<TagRespDto> getTag(Long id);

    /**
     * TagGroup별 태그 목록 조회
     *
     * <pre>
     * tagGroupId != null → 해당 그룹의 태그 목록
     * tagGroupId == null → 전체 태그 목록
     * </pre>
     */
    List<TagRespDto> getTags(Long tagGroupId);

    TagRespDto updateTag(Long id, TagUpdateReqDto dto);

    void deleteTag(Long id);
}

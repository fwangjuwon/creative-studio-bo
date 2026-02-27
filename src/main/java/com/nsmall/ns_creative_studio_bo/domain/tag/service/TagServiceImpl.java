package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagGroupMapper;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tag 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final TagGroupMapper tagGroupMapper;

    @Override
    public TagRespDto createTag(TagCreateReqDto dto) {
        tagGroupMapper.selectById(dto.getTagGroupId())
                .orElseThrow(() -> new BizException(TagErrorCode.TAG_GROUP_NOT_FOUND,
                        "태그 그룹을 찾을 수 없습니다. tagGroupId=" + dto.getTagGroupId()));

        LocalDateTime now = LocalDateTime.now();
        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        Tag tag = Tag.builder()
                .tagGroupId(dto.getTagGroupId())
                .name(dto.getName())
                .sortOrder(dto.getSortOrder())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        tagMapper.insert(tag);

        return tagMapper.selectById(tag.getId())
                .map(TagRespDto::from)
                .orElse(null);
    }

    @Override
    public Optional<TagRespDto> getTag(Long id) {
        return tagMapper.selectById(id).map(TagRespDto::from);
    }

    @Override
    public List<TagRespDto> getTags(Long tagGroupId) {
        return tagMapper.selectList(tagGroupId).stream()
                .map(TagRespDto::from)
                .toList();
    }

    @Override
    public TagRespDto updateTag(Long id, TagUpdateReqDto dto) {
        tagMapper.selectById(id)
                .orElseThrow(() -> new BizException(TagErrorCode.TAG_NOT_FOUND,
                        "태그를 찾을 수 없습니다. id=" + id));

        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        Tag updated = Tag.builder()
                .id(id)
                .name(dto.getName())
                .sortOrder(dto.getSortOrder())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        tagMapper.update(updated);

        return tagMapper.selectById(id)
                .map(TagRespDto::from)
                .orElse(null);
    }

    @Override
    public void deleteTag(Long id) {
        tagMapper.selectById(id)
                .orElseThrow(() -> new BizException(TagErrorCode.TAG_NOT_FOUND,
                        "태그를 찾을 수 없습니다. id=" + id));

        tagMapper.deleteById(id);
    }
}

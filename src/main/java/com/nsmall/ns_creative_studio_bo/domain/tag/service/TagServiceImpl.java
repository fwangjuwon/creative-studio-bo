package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.entity.Tag;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.GoodsPromptHistoryTagMapper;
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
    private final GoodsPromptHistoryTagMapper goodsPromptHistoryTagMapper;

    @Override
    public TagRespDto createTag(TagCreateReqDto dto) {
        tagGroupMapper.selectById(dto.getTagGroupId())
                .orElseThrow(() -> new BizException(TagErrorCode.TAG_GROUP_NOT_FOUND,
                        "태그 그룹을 찾을 수 없습니다. tagGroupId=" + dto.getTagGroupId()));

        // sortOrder 미입력 시 해당 그룹 내 최대값 + 1 자동 세팅
        int sortOrder = dto.getSortOrder() != null
                ? dto.getSortOrder()
                : tagMapper.selectMaxSortOrder(dto.getTagGroupId()) + 1;

        if (dto.getSortOrder() != null
                && tagMapper.existsBySortOrder(dto.getTagGroupId(), sortOrder)) {
            throw new BizException(TagErrorCode.DUPLICATE_SORT_ORDER,
                    "동일 그룹 내에 정렬순서 " + sortOrder + "이(가) 이미 존재합니다.");
        }

        if (tagMapper.existsByNameAndTagGroupId(dto.getName(), dto.getTagGroupId(), null)) {
            throw new BizException(TagErrorCode.DUPLICATE_TAG_NAME,
                    "이미 존재하는 태그명입니다. name=" + dto.getName());
        }

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        Tag tag = Tag.builder()
                .tagGroupId(dto.getTagGroupId())
                .name(dto.getName())
                .tagValue(dto.getTagValue())
                .sortOrder(sortOrder)
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
        Tag existing = tagMapper.selectById(id)
                .orElseThrow(() -> new BizException(TagErrorCode.TAG_NOT_FOUND,
                        "태그를 찾을 수 없습니다. id=" + id));

        if (dto.getSortOrder() != null
                && tagMapper.existsBySortOrderExcludingId(id, existing.getTagGroupId(), dto.getSortOrder())) {
            throw new BizException(TagErrorCode.DUPLICATE_SORT_ORDER,
                    "동일 그룹 내에 정렬순서 " + dto.getSortOrder() + "이(가) 이미 존재합니다.");
        }
        if (tagMapper.existsByNameAndTagGroupId(dto.getName(), existing.getTagGroupId(), id)) {
            throw new BizException(TagErrorCode.DUPLICATE_TAG_NAME,
                    "이미 존재하는 태그명입니다. name=" + dto.getName());
        }

        String requesterId = SecurityUtil.getCurrentLoginId();

        Tag updated = Tag.builder()
                .id(id)
                .name(dto.getName())
                .tagValue(dto.getTagValue())
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

        if (goodsPromptHistoryTagMapper.existsByTagId(id)) {
            throw new BizException(TagErrorCode.TAG_IN_USE,
                    "프롬프트 히스토리에서 사용 중인 태그는 삭제할 수 없습니다. tagId=" + id);
        }

        tagMapper.deleteById(id);
    }
}

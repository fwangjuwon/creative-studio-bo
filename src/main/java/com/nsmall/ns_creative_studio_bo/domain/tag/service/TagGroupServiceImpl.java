package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagGroupErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.entity.TagGroup;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagGroupMapper;
import com.nsmall.ns_creative_studio_bo.domain.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TagGroup 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagGroupServiceImpl implements TagGroupService {

    private final TagGroupMapper tagGroupMapper;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public TagGroupRespDto createTagGroup(TagGroupCreateReqDto dto) {

        // sortOrder 미입력 시 현재 최대값 + 1 자동 세팅
        int sortOrder = dto.getSortOrder() != null
                ? dto.getSortOrder()
                : tagGroupMapper.selectMaxSortOrder() + 1;

        if (dto.getSortOrder() != null
                && tagGroupMapper.existsBySortOrder(sortOrder)) {
            throw new BizException(TagGroupErrorCode.DUPLICATE_SORT_ORDER,
                    "이미 정렬순서 " + sortOrder + "을(를) 가진 태그 그룹이 존재합니다.");
        }
        if (tagGroupMapper.existsByName(dto.getName(), null)) {
            throw new BizException(TagGroupErrorCode.DUPLICATE_TAG_GROUP_NAME,
                    "이미 존재하는 태그 그룹명입니다. name=" + dto.getName());
        }

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        TagGroup tagGroup = TagGroup.builder()
                .name(dto.getName())
                .isMultiSelect(dto.getIsMultiSelect())
                .sortOrder(sortOrder)
                .type(dto.getType())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        tagGroupMapper.insert(tagGroup);

        return tagGroupMapper.selectById(tagGroup.getId())
                .map(TagGroupRespDto::from)
                .orElse(null);
    }

    @Override
    public Optional<TagGroupRespDto> getTagGroup(Long id) {
        return tagGroupMapper.selectById(id).map(TagGroupRespDto::from);
    }

    @Override
    public List<TagGroupRespDto> getTagGroups() {
        return tagGroupMapper.selectList().stream()
                .map(TagGroupRespDto::from)
                .toList();
    }

    @Override
    public List<TagGroupRespDto> getTagGroups(String type) {
        return tagGroupMapper.selectListByType(type).stream()
                .map(TagGroupRespDto::from)
                .toList();
    }

    @Override
    @Transactional
    public TagGroupRespDto updateTagGroup(Long id, TagGroupUpdateReqDto dto) {
        tagGroupMapper.selectById(id)
                .orElseThrow(() -> new BizException(TagGroupErrorCode.TAG_GROUP_NOT_FOUND,
                        "태그 그룹을 찾을 수 없습니다. id=" + id));


        if (dto.getSortOrder() != null
                && tagGroupMapper.existsBySortOrderExcludingId(id, dto.getSortOrder())) {
            throw new BizException(TagGroupErrorCode.DUPLICATE_SORT_ORDER,
                    "이미 정렬순서 " + dto.getSortOrder() + "을(를) 가진 태그 그룹이 존재합니다.");
        }
        if (tagGroupMapper.existsByName(dto.getName(), id)) {
            throw new BizException(TagGroupErrorCode.DUPLICATE_TAG_GROUP_NAME,
                    "이미 존재하는 태그 그룹명입니다. name=" + dto.getName());
        }

        String requesterId = SecurityUtil.getCurrentLoginId();

        TagGroup updated = TagGroup.builder()
                .id(id)
                .name(dto.getName())
                .isMultiSelect(dto.getIsMultiSelect())
                .sortOrder(dto.getSortOrder())
                .type(dto.getType())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        tagGroupMapper.update(updated);

        return tagGroupMapper.selectById(id)
                .map(TagGroupRespDto::from)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteTagGroup(Long id) {
        tagGroupMapper.selectById(id)
                .orElseThrow(() -> new BizException(TagGroupErrorCode.TAG_GROUP_NOT_FOUND,
                        "태그 그룹을 찾을 수 없습니다. id=" + id));

        if (tagMapper.existsByTagGroupId(id)) {
            throw new BizException(TagGroupErrorCode.TAG_GROUP_HAS_TAGS,
                    "소속 태그가 존재하여 태그 그룹을 삭제할 수 없습니다. id=" + id);
        }

        tagGroupMapper.deleteById(id);
    }
}

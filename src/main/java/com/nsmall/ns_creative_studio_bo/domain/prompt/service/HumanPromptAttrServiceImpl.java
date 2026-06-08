package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptAttrSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptAttr;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptAttrMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 세부항목 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HumanPromptAttrServiceImpl implements HumanPromptAttrService {

    private final HumanPromptAttrMapper humanPromptAttrMapper;
    private final HumanPromptStepMapper humanPromptStepMapper;
    private final HumanPromptOptionMapper humanPromptOptionMapper;

    @Override
    @Transactional
    public HumanPromptAttrSimpleRespDto createAttr(HumanPromptAttrCreateReqDto dto) {
        humanPromptStepMapper.selectById(dto.getStepId())
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.STEP_NOT_FOUND,
                        "단계를 찾을 수 없습니다. stepId=" + dto.getStepId()));

        if (humanPromptAttrMapper.existsByNameAndStepId(dto.getName(), dto.getStepId(), null)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_ATTR_NAME,
                    "이미 존재하는 세부항목명입니다. name=" + dto.getName());
        }

        // sortOrder 미입력 시 해당 step 내 최대값 + 1 자동 세팅
        int sortOrder = dto.getSortOrder() != null
                ? dto.getSortOrder()
                : humanPromptAttrMapper.selectMaxSortOrder(dto.getStepId()) + 1;

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptAttr attr = HumanPromptAttr.builder()
                .stepId(dto.getStepId())
                .name(dto.getName())
                .isMultiSelect(dto.getIsMultiSelect())
                .isRequired(dto.getIsRequired())
                .sortOrder(sortOrder)
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        humanPromptAttrMapper.insert(attr);

        return humanPromptAttrMapper.selectById(attr.getId())
                .map(HumanPromptAttrSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    @Transactional
    public HumanPromptAttrSimpleRespDto updateAttr(Long id, HumanPromptAttrUpdateReqDto dto) {
        HumanPromptAttr existing = humanPromptAttrMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.ATTR_NOT_FOUND,
                        "세부항목을 찾을 수 없습니다. id=" + id));

        if (humanPromptAttrMapper.existsByNameAndStepId(dto.getName(), existing.getStepId(), id)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_ATTR_NAME,
                    "이미 존재하는 세부항목명입니다. name=" + dto.getName());
        }

        // 필수 항목을 해제하려는 경우, 해당 step에 남은 필수 attr이 없으면 거부
        if (existing.isRequired() && !dto.getIsRequired()) {
            int remainingRequired = humanPromptAttrMapper.countRequiredByStepId(existing.getStepId(), id);
            if (remainingRequired == 0) {
                throw new BizException(HumanPromptErrorCode.ATTR_LAST_REQUIRED,
                        "단계에 필수 세부항목이 최소 1개 이상 있어야 합니다. stepId=" + existing.getStepId());
            }
        }

        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptAttr updated = HumanPromptAttr.builder()
                .id(id)
                .name(dto.getName())
                .isMultiSelect(dto.getIsMultiSelect())
                .isRequired(dto.getIsRequired())
                .sortOrder(dto.getSortOrder())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        humanPromptAttrMapper.update(updated);

        return humanPromptAttrMapper.selectById(id)
                .map(HumanPromptAttrSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteAttr(Long id) {
        HumanPromptAttr existing = humanPromptAttrMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.ATTR_NOT_FOUND,
                        "세부항목을 찾을 수 없습니다. id=" + id));

        if (humanPromptOptionMapper.existsByAttrId(id)) {
            throw new BizException(HumanPromptErrorCode.ATTR_HAS_OPTIONS,
                    "하위 선택지가 존재하여 세부항목을 삭제할 수 없습니다. id=" + id);
        }

        // 필수 attr을 삭제하려는 경우, step에 남은 필수 attr이 없으면 거부
        if (existing.isRequired()) {
            int remainingRequired = humanPromptAttrMapper.countRequiredByStepId(existing.getStepId(), id);
            if (remainingRequired == 0) {
                throw new BizException(HumanPromptErrorCode.ATTR_LAST_REQUIRED,
                        "단계에 필수 세부항목이 최소 1개 이상 있어야 합니다. stepId=" + existing.getStepId());
            }
        }

        humanPromptAttrMapper.deleteById(id);
    }
}

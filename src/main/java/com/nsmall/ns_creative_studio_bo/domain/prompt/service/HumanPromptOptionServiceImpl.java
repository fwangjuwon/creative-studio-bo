package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptOptionSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptAttrMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptHistoryOptionMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptOptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 선택지 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Service
@RequiredArgsConstructor
public class HumanPromptOptionServiceImpl implements HumanPromptOptionService {

    private final HumanPromptOptionMapper humanPromptOptionMapper;
    private final HumanPromptAttrMapper humanPromptAttrMapper;
    private final HumanPromptHistoryOptionMapper humanPromptHistoryOptionMapper;

    @Override
    public HumanPromptOptionSimpleRespDto createOption(HumanPromptOptionCreateReqDto dto) {
        humanPromptAttrMapper.selectById(dto.getAttrId())
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.ATTR_NOT_FOUND,
                        "세부항목을 찾을 수 없습니다. attrId=" + dto.getAttrId()));

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptOption option = new HumanPromptOption();
        option.setAttrId(dto.getAttrId());
        option.setName(dto.getName());
        option.setPromptValue(dto.getPromptValue());
        option.setSortOrder(dto.getSortOrder());
        option.setInitRegiPic(requesterId);
        option.setInitRegiDttm(now);
        option.setFinalRvisPic(requesterId);
        option.setFinalRvisDttm(now);

        humanPromptOptionMapper.insert(option);

        return humanPromptOptionMapper.selectById(option.getId())
                .map(HumanPromptOptionSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    public HumanPromptOptionSimpleRespDto updateOption(Long id, HumanPromptOptionUpdateReqDto dto) {
        humanPromptOptionMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.OPTION_NOT_FOUND,
                        "선택지를 찾을 수 없습니다. id=" + id));

        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptOption option = new HumanPromptOption();
        option.setId(id);
        option.setName(dto.getName());
        option.setPromptValue(dto.getPromptValue());
        option.setSortOrder(dto.getSortOrder());
        option.setFinalRvisPic(requesterId);
        option.setFinalRvisDttm(LocalDateTime.now());

        humanPromptOptionMapper.update(option);

        return humanPromptOptionMapper.selectById(id)
                .map(HumanPromptOptionSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    public void deleteOption(Long id) {
        humanPromptOptionMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.OPTION_NOT_FOUND,
                        "선택지를 찾을 수 없습니다. id=" + id));

        if (humanPromptHistoryOptionMapper.existsByOptionId(id)) {
            throw new BizException(HumanPromptErrorCode.OPTION_IN_USE,
                    "프롬프트 이력에서 사용 중인 선택지는 삭제할 수 없습니다. id=" + id);
        }

        humanPromptOptionMapper.deleteById(id);
    }
}

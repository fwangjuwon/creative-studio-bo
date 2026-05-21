package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptStep;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptAttrMapper;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 단계 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Service
@RequiredArgsConstructor
public class HumanPromptStepServiceImpl implements HumanPromptStepService {

    private final HumanPromptStepMapper humanPromptStepMapper;
    private final HumanPromptAttrMapper humanPromptAttrMapper;

    @Override
    public HumanPromptStepSimpleRespDto createStep(HumanPromptStepCreateReqDto dto) {
        if (humanPromptStepMapper.existsByName(dto.getName(), null)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_STEP_NAME,
                    "이미 존재하는 단계명입니다. name=" + dto.getName());
        }

        // stepNumber 미입력 시 현재 최대값 + 1 자동 세팅
        int stepNumber = dto.getStepNumber() != null
                ? dto.getStepNumber()
                : humanPromptStepMapper.selectMaxStepNumber() + 1;

        // stepNumber 중복 체크 (직접 입력한 경우만)
        if (dto.getStepNumber() != null
                && humanPromptStepMapper.existsByStepNumber(stepNumber, null)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_STEP_NUMBER,
                    "이미 존재하는 순서 번호입니다. stepNumber=" + stepNumber);
        }

        // stepCode 미입력 시 stepNumber 기반 알파벳 자동 생성 (1→A, 2→B, ...)
        // 명시 입력 시 toUpperCase 처리 + 중복 체크
        String stepCode = (dto.getStepCode() != null && !dto.getStepCode().isBlank())
                ? dto.getStepCode().toUpperCase()
                : String.valueOf((char) ('A' + stepNumber - 1));

        // stepCode 중복 체크 (직접 입력한 경우만)
        if (dto.getStepCode() != null && !dto.getStepCode().isBlank()
                && humanPromptStepMapper.existsByStepCode(stepCode, null)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_STEP_CODE,
                    "이미 존재하는 단계 코드입니다. stepCode=" + stepCode);
        }

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptStep step = HumanPromptStep.builder()
                .stepNumber(stepNumber)
                .stepCode(stepCode)
                .name(dto.getName())
                .isCommon(dto.isCommon())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        humanPromptStepMapper.insert(step);

        return humanPromptStepMapper.selectById(step.getId())
                .map(HumanPromptStepSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    public HumanPromptStepSimpleRespDto updateStep(Long id, HumanPromptStepUpdateReqDto dto) {
        humanPromptStepMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.STEP_NOT_FOUND,
                        "단계를 찾을 수 없습니다. id=" + id));

        if (humanPromptStepMapper.existsByName(dto.getName(), id)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_STEP_NAME,
                    "이미 존재하는 단계명입니다. name=" + dto.getName());
        }
        if (humanPromptStepMapper.existsByStepCode(dto.getStepCode(), id)) {
            throw new BizException(HumanPromptErrorCode.DUPLICATE_STEP_CODE,
                    "이미 존재하는 단계 코드입니다. stepCode=" + dto.getStepCode());
        }

        String requesterId = SecurityUtil.getCurrentLoginId();

        HumanPromptStep updated = HumanPromptStep.builder()
                .id(id)
                .stepNumber(dto.getStepNumber())
                .stepCode(dto.getStepCode().toUpperCase())
                .name(dto.getName())
                .isCommon(dto.isCommon())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        humanPromptStepMapper.update(updated);

        return humanPromptStepMapper.selectById(id)
                .map(HumanPromptStepSimpleRespDto::from)
                .orElse(null);
    }

    @Override
    public void deleteStep(Long id) {
        humanPromptStepMapper.selectById(id)
                .orElseThrow(() -> new BizException(HumanPromptErrorCode.STEP_NOT_FOUND,
                        "단계를 찾을 수 없습니다. id=" + id));

        if (humanPromptAttrMapper.existsByStepId(id)) {
            throw new BizException(HumanPromptErrorCode.STEP_HAS_ATTRS,
                    "하위 세부항목이 존재하여 단계를 삭제할 수 없습니다. id=" + id);
        }

        humanPromptStepMapper.deleteById(id);
    }
}

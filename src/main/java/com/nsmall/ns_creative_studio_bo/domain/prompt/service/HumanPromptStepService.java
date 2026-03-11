package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepSimpleRespDto;

/**
 * 인간 이미지 프롬프트 단계 서비스 인터페이스
 *
 * <pre>
 * Step CUD를 담당한다.
 * 조회(getSteps)는 기존 HumanPromptService에서 담당.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
public interface HumanPromptStepService {

    HumanPromptStepSimpleRespDto createStep(HumanPromptStepCreateReqDto dto);

    HumanPromptStepSimpleRespDto updateStep(Long id, HumanPromptStepUpdateReqDto dto);

    void deleteStep(Long id);
}

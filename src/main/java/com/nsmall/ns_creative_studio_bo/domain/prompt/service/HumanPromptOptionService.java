package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptOptionSimpleRespDto;

/**
 * 인간 이미지 프롬프트 선택지 서비스 인터페이스
 *
 * <pre>
 * Option CUD를 담당한다.
 * 조회는 기존 HumanPromptService에서 담당.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
public interface HumanPromptOptionService {

    HumanPromptOptionSimpleRespDto createOption(HumanPromptOptionCreateReqDto dto);

    HumanPromptOptionSimpleRespDto updateOption(Long id, HumanPromptOptionUpdateReqDto dto);

    void deleteOption(Long id);
}

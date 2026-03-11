package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptAttrSimpleRespDto;

/**
 * 인간 이미지 프롬프트 세부항목 서비스 인터페이스
 *
 * <pre>
 * Attr CUD를 담당한다.
 * 조회는 기존 HumanPromptService에서 담당.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
public interface HumanPromptAttrService {

    HumanPromptAttrSimpleRespDto createAttr(HumanPromptAttrCreateReqDto dto);

    HumanPromptAttrSimpleRespDto updateAttr(Long id, HumanPromptAttrUpdateReqDto dto);

    void deleteAttr(Long id);
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;

import java.util.List;

/**
 * 인간 이미지 프롬프트 서비스 인터페이스
 *
 * <pre>
 * 5단계 구조 조회 및 인간 이미지 전용 프롬프트 생성을 담당한다.
 * 히스토리 저장 없음 (MVP 단계).
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
public interface HumanPromptService {

    /**
     * 인간 이미지 프롬프트 생성 전체 구조를 조회한다.
     * steps → attrs → options 중첩 형태로 반환.
     *
     * @return 단계 응답 DTO 목록
     */
    List<HumanPromptStepRespDto> getSteps();

    /**
     * 선택된 옵션 기반으로 인간 이미지 프롬프트를 생성한다.
     * 히스토리 저장 없음 (daily limit 체크 없음).
     *
     * @param dto 선택 항목 맵 + 생성 목적 + 추가 지시
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String createPrompt(HumanPromptCreateReqDto dto);
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 인간 이미지 프롬프트 서비스 인터페이스
 *
 * <pre>
 * 5단계 구조 조회 및 인간 이미지 전용 프롬프트 생성을 담당한다.
 * 프롬프트 생성 시 prompt_history 테이블에 이력 저장 (promptType='PERSON').
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
     * 선택된 옵션 기반으로 인간 이미지 프롬프트를 생성하고 이력을 저장한다.
     *
     * @param userId 현재 로그인 사용자 ID
     * @param dto    선택 항목 맵 + 생성 목적 + 추가 지시
     * @return 생성된 이력 DTO (id + promptText 포함)
     */
    PromptHistoryRespDto createPrompt(Long userId, HumanPromptCreateReqDto dto);

    /**
     * NanoBanana 이미지를 3개 병렬 생성하고, 첫 번째 URL을 이력에 업데이트한다.
     * referenceImage 가 전달되면 해당 이미지를 Base64 로 인코딩하여 함께 전달한다.
     *
     * @param historyId      업데이트할 prompt_history ID
     * @param promptText     이미지 생성에 사용할 영문 프롬프트
     * @param referenceImage 참조 이미지 (선택, 주로 상품 이미지). 인물 묘사는 promptText 에 따라 새로 생성
     * @return 생성된 이미지 접근 URL 목록 (/images/{uuid}.jpg × 3)
     */
    List<String> generateImage(Long historyId, String promptText, MultipartFile referenceImage);
}

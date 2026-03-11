package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;

import java.util.List;

/**
 * Prompt 서비스 인터페이스
 *
 * <pre>
 * 프롬프트 생성, 일일 한도 체크, 히스토리 조회를 담당한다.
 * 히스토리 조회는 요청 사용자의 권한에 따라 범위가 달라진다:
 *   - USER  → 같은 팀(team_name) 유저들의 히스토리만 조회
 *   - MANAGER / ADMIN → 전체 히스토리 조회
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface PromptService {

    /**
     * 상품(Goods) 프롬프트를 생성하고 히스토리를 저장한다.
     *
     * @param currentUserId JWT SecurityContext에서 추출한 현재 로그인 사용자 ID
     * @param dto           상품 프롬프트 생성 요청 DTO (goodsCategoryId, tagIds)
     * @return 생성된 프롬프트 히스토리 응답 DTO
     */
    PromptHistoryRespDto createGoodsPrompt(Long currentUserId, GoodsPromptCreateReqDto dto);

    /**
     * 요청 사용자의 권한에 따라 프롬프트 히스토리 목록을 조회한다.
     *
     * @param requestUserId 요청한 사용자 ID
     * @return 프롬프트 히스토리 응답 DTO 목록
     */
    List<PromptHistoryRespDto> getPromptHistories(Long requestUserId);

    /**
     * NanoBanana 이미지를 3개 병렬 생성하고, 첫 번째 URL을 이력에 업데이트한다.
     *
     * @param historyId  업데이트할 prompt_history ID (null이면 이력 업데이트 생략)
     * @param promptText 이미지 생성에 사용할 영문 프롬프트
     * @return 생성된 이미지 접근 URL 목록 (/images/{uuid}.jpg × 3)
     */
    List<String> generateGoodsImage(Long historyId, String promptText);
}

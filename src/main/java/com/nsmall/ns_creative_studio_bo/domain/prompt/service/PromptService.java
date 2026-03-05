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
}

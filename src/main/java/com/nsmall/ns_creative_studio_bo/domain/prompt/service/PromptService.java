package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Prompt 서비스 인터페이스
 *
 * <pre>
 * 프롬프트 생성, 일일 한도 체크, 히스토리 조회를 담당한다.
 * 히스토리 조회는 Role 구분 없이 전체 이력을 반환한다.
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
     * 프롬프트 히스토리 전체 목록을 조회한다. Role 구분 없이 동일한 결과를 반환한다.
     *
     * @return 프롬프트 히스토리 응답 DTO 목록
     */
    List<PromptHistoryRespDto> getPromptHistories();

    /**
     * NanoBanana 이미지를 3개 병렬 생성하고 URL 목록을 이력에 저장한다.
     * 참조 이미지가 있으면 참조 이미지 기반으로, 없으면 텍스트만으로 생성한다.
     *
     * @param historyId      업데이트할 prompt_history ID (null이면 이력 업데이트 생략)
     * @param promptText     이미지 생성에 사용할 영문 프롬프트
     * @param referenceImage 참조 이미지 파일 (null 허용 — null이면 텍스트만으로 생성)
     * @return 생성된 이미지 접근 URL 목록 (/images/{uuid}.jpg × 3)
     */
    List<String> generateGoodsImage(Long historyId, String promptText, MultipartFile referenceImage);

}

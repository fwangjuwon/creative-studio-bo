package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptHistorySearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Prompt 서비스 인터페이스
 *
 * <pre>
 * 프롬프트 생성, 일일 한도 체크, 히스토리 조회 및 삭제를 담당한다.
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
     * 프롬프트 히스토리 목록을 검색 조건과 페이지 단위로 조회한다. Role 구분 없이 동일한 결과를 반환한다.
     *
     * @param searchReqDto 검색 + 페이징 요청 (loginId, promptType, promptStyle, dateFrom, dateTo, pageNum, pageSize)
     * @return 페이징 처리된 프롬프트 히스토리 응답
     */
    PageRespDto<PromptHistoryRespDto> getPromptHistories(PromptHistorySearchReqDto searchReqDto);

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

    /**
     * 프롬프트 이력을 삭제한다. ADMIN 전용.
     *
     * <pre>
     * 삭제 순서 (@Transactional 내 동일 커밋 단위):
     *   1. PRODUCT → goods_prompt_history_tag 삭제
     *      PERSON  → human_prompt_history_option 삭제
     *   2. prompt_history 삭제
     *   3. imageUrl 파싱 후 디스크 이미지 파일 삭제 (null/빈값이면 스킵)
     *      ※ 파일 삭제는 롤백 불가 — DB 작업과 동일 트랜잭션 내에서 실행됨
     * </pre>
     *
     * @param id 삭제할 prompt_history ID
     */
    void deleteHistory(Long id);

}

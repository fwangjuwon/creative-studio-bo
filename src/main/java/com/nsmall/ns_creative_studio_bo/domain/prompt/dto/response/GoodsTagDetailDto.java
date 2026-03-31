package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 프롬프트 히스토리 목록 조회 시 태그 배치 조회 결과 DTO
 *
 * <pre>
 * ※ 내부 전용 DTO — API 응답에 직접 노출되지 않음.
 *    Mapper → Service 계층 간 배치 조회 결과 전달 목적으로만 사용한다.
 *
 * N+1 방지 목적으로 사용.
 * goods_prompt_history_tag JOIN tag 결과를 담아
 * Service에서 promptHistoryId 기준으로 groupingBy 처리한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-31
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsTagDetailDto {

    private Long promptHistoryId;
    private Long tagId;
    private String tagName;
}

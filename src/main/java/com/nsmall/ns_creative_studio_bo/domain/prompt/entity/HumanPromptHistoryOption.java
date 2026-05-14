package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * HumanPromptHistoryOption 엔티티 — {@code human_prompt_history_option} 테이블 매핑
 *
 * <pre>
 * 인물 프롬프트 생성 시 사용자가 선택한 옵션 매핑 테이블 (인물 프롬프트 전용).
 * prompt_history_id: 논리 FK → PromptHistory (DB 제약 없음)
 * option_id        : 논리 FK → HumanPromptOption (DB 제약 없음)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-09
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptHistoryOption {

    private Long id;
    private Long promptHistoryId;
    private Long optionId;
    /**
     * 인물 인덱스.
     * -1: 공통 단계 옵션 (조명/배경, 촬영기술)
     *  0 이상: 인물별 개별 옵션 (0 = 사람1, 1 = 사람2, ...)
     *  6명 이상 단체 모드의 피사체 옵션은 0으로 저장
     */
    private Integer personIndex;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

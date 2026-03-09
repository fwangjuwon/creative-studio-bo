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

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

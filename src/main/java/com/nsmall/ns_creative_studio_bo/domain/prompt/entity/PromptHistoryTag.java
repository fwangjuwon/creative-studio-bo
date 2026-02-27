package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PromptHistoryTag 엔티티 — {@code prompt_history_tag} 테이블 매핑
 *
 * <pre>
 * 프롬프트와 태그의 다대다 매핑 테이블.
 * prompt_history_id: 논리 FK → PromptHistory (DB 제약 없음)
 * tag_id: 논리 FK → Tag (DB 제약 없음)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistoryTag {

    private Long id;
    private Long promptHistoryId;
    private Long tagId;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

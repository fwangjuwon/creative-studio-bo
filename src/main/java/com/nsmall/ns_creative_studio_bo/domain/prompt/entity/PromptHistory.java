package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PromptHistory 엔티티 — {@code prompt_history} 테이블 매핑
 *
 * <pre>
 * 사용자가 생성한 프롬프트 이력.
 * user_id: 논리 FK → User (DB 제약 없음)
 * goods_category_id: 논리 FK → Category DCS (DB 제약 없음, 상품군)
 * init_regi_dttm 기준으로 일일 사용량을 집계한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistory {

    private Long id;
    private Long userId;
    private Long goodsCategoryId;
    private String promptText;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

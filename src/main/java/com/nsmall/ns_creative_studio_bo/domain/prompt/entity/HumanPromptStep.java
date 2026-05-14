package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 생성 단계 엔티티
 *
 * <pre>
 * human_prompt_step 테이블 매핑.
 * 5단계 (A~E): 핵심 피사체 / 분위기·스타일 / 리얼리즘 / 조명·배경 / 촬영기술
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStep {

    private Long id;
    private int stepNumber;      // 1~5
    private String stepCode;     // A/B/C/D/E
    private String name;
    /** true: 전체 공통 선택 (조명/촬영) / false: 인물별 개별 선택 (피사체/분위기/리얼리즘) */
    private boolean isCommon;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

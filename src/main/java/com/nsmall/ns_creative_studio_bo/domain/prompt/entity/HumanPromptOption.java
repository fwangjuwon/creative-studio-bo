package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 선택지 엔티티
 *
 * <pre>
 * human_prompt_option 테이블 매핑.
 * name: 한국어 표시명 (UI 노출)
 * promptValue: Gemini API에 전달할 영문 값
 * stepNumber: JOIN 조회 시 포함 — 서비스 레이어에서 stepNumber별 그룹핑에 사용
 *
 * @Builder 미사용 — MyBatis가 기본 생성자 + setter로 결과를 매핑한다.
 * (@Builder 사용 시 전체 필드 생성자로 auto-mapping을 시도해 컬럼 수 불일치 오류 발생)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@Setter
@NoArgsConstructor
public class HumanPromptOption {

    private Long id;
    private Long attrId;
    private String name;
    private String promptValue;
    private int sortOrder;

    /** JOIN 조회 시 포함 — human_prompt_step.step_number */
    private Integer stepNumber;
    /** JOIN 조회 시 포함 — human_prompt_step.name (STEP_LABELS 하드코딩 대체) */
    private String stepName;
    /** JOIN 조회 시 포함 — human_prompt_attr.name (Gemini 프롬프트에 속성명 구분용) */
    private String attrName;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

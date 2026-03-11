package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인간 이미지 프롬프트 세부 항목 엔티티
 *
 * <pre>
 * human_prompt_attr 테이블 매핑.
 * 각 Step에 속하는 세부 항목 (연령대, 성별, 국적/인종 등).
 * isMultiSelect: 복수 선택 허용 여부
 * isRequired: 필수 선택 여부
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptAttr {

    private Long id;
    private Long stepId;
    private String name;
    private boolean isMultiSelect;
    private boolean isRequired;
    private int sortOrder;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

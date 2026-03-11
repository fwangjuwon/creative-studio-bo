package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptAttr;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 세부항목 단순 응답 DTO
 *
 * <pre>
 * Attr CUD API 응답에 사용.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptAttrSimpleRespDto {

    private Long id;
    private Long stepId;
    private String name;
    private boolean isMultiSelect;
    private boolean isRequired;
    private int sortOrder;

    public static HumanPromptAttrSimpleRespDto from(HumanPromptAttr attr) {
        return HumanPromptAttrSimpleRespDto.builder()
                .id(attr.getId())
                .stepId(attr.getStepId())
                .name(attr.getName())
                .isMultiSelect(attr.isMultiSelect())
                .isRequired(attr.isRequired())
                .sortOrder(attr.getSortOrder())
                .build();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 단계 수정 요청 DTO
 *
 * <pre>
 * stepCode는 단계 식별자이므로 수정 불가.
 * stepNumber, name만 수정 가능.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStepUpdateReqDto {

    private Integer stepNumber;
    private String name;
}

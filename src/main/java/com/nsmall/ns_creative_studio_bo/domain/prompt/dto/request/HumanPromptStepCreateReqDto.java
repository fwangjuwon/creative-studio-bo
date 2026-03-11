package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 단계 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStepCreateReqDto {

    @NotNull(message = "단계 번호는 필수입니다.")
    private Integer stepNumber;

    @NotBlank(message = "단계 코드는 필수입니다.")
    private String stepCode;

    @NotBlank(message = "단계명은 필수입니다.")
    private String name;
}

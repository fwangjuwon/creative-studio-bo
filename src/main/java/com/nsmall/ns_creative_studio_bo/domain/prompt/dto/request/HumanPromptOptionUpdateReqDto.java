package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 선택지 수정 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptOptionUpdateReqDto {

    @NotBlank(message = "선택지명은 필수입니다.")
    @Size(max = 200, message = "선택지명은 200자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "프롬프트 값은 필수입니다.")
    @Size(max = 500, message = "프롬프트 값은 500자 이하여야 합니다.")
    private String promptValue;

    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;
}

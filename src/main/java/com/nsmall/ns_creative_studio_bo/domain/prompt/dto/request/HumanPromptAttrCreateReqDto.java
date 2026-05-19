package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 세부항목 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptAttrCreateReqDto {

    @NotNull(message = "단계 ID는 필수입니다.")
    private Long stepId;

    @NotBlank(message = "세부항목명은 필수입니다.")
    @Size(max = 100, message = "세부항목명은 100자 이하여야 합니다.")
    private String name;

    @NotNull(message = "복수 선택 허용 여부는 필수입니다.")
    private Boolean isMultiSelect;

    @NotNull(message = "필수 선택 여부는 필수입니다.")
    private Boolean isRequired;

    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;
}

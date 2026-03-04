package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Getter
@NoArgsConstructor
public class ImageGenerateReqDto {

    @NotBlank(message = "promptText는 필수입니다.")
    private String promptText;
}

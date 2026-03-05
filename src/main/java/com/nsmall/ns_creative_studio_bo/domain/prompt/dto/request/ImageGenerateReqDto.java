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

    /** 이력 업데이트용 — 프롬프트 생성 후 반환된 historyId (없으면 이력 업데이트 생략) */
    private Long historyId;

    @NotBlank(message = "promptText는 필수입니다.")
    private String promptText;
}

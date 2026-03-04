package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 이미지 생성 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Getter
@Builder
public class ImageGenerateRespDto {

    /** 생성된 이미지 접근 URL — 예: /images/{uuid}.jpg */
    private String imageUrl;
}

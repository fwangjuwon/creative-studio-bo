package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 이미지 생성 응답 DTO
 *
 * <pre>
 * 하나의 프롬프트로 생성된 이미지 URL 목록을 반환한다.
 * 기본 생성 개수는 3개이며, imageUrls에 순서대로 담긴다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Getter
@Builder
public class ImageGenerateRespDto {

    /** 생성된 이미지 접근 URL 목록 — 예: ["/images/{uuid1}.jpg", "/images/{uuid2}.jpg", ...] */
    private List<String> imageUrls;
}

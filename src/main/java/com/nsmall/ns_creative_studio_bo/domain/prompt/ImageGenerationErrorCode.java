package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이미지 생성 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Getter
@RequiredArgsConstructor
public enum ImageGenerationErrorCode implements ErrorCode {

    EMPTY_IMAGE_RESPONSE("IG001", "Gemini 이미지 응답이 비어 있습니다.", 500),
    IMAGE_SAVE_FAILED("IG002", "이미지 저장에 실패했습니다.", 500),
    IMAGE_GENERATION_FAILED("IG003", "이미지 생성에 실패했습니다.", 500),
    INVALID_IMAGE_MIME_TYPE("IG004", "참조 이미지의 Content-Type을 확인할 수 없습니다.", 400),
    UNSUPPORTED_IMAGE_TYPE("IG005", "지원하지 않는 이미지 형식입니다. (jpeg, png, webp만 허용)", 400),
    IMAGE_TOO_LARGE("IG006", "참조 이미지 크기가 허용 범위를 초과합니다. (최대 15MB)", 400),
    IMAGE_GENERATION_RATE_LIMITED("IG007", "이미지 생성 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", 500),
    IMAGE_GENERATION_AUTH_FAILED("IG008", "이미지 생성 서비스 인증에 실패했습니다. 관리자에게 문의해 주세요.", 500),
    IMAGE_GENERATION_UNAVAILABLE("IG009", "이미지 생성 서비스가 일시적으로 이용 불가합니다. 잠시 후 다시 시도해 주세요.", 500);

    private final String code;
    private final String message;
    private final int httpStatus;
}

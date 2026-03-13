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
    INVALID_IMAGE_MIME_TYPE("IG004", "참조 이미지의 Content-Type을 확인할 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

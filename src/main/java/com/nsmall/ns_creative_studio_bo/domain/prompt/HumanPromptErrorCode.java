package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인간 이미지 프롬프트 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@RequiredArgsConstructor
public enum HumanPromptErrorCode implements ErrorCode {

    NO_OPTIONS_SELECTED("HP001", "선택된 옵션이 없습니다.", 400),
    GEMINI_EMPTY_RESPONSE("HP002", "Gemini API로부터 유효한 응답을 받지 못했습니다.", 500),

    STEP_NOT_FOUND("HP010", "존재하지 않는 단계입니다.", 400),
    STEP_HAS_ATTRS("HP011", "하위 세부항목이 존재하여 삭제할 수 없습니다.", 400),

    ATTR_NOT_FOUND("HP012", "존재하지 않는 세부항목입니다.", 400),
    ATTR_HAS_OPTIONS("HP013", "하위 선택지가 존재하여 삭제할 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

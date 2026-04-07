package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Prompt 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@RequiredArgsConstructor
public enum PromptErrorCode implements ErrorCode {

    DAILY_LIMIT_EXCEEDED("P001", "일일 프롬프트 생성 한도를 초과했습니다.", 400),
    USER_NOT_FOUND("P002", "사용자를 찾을 수 없습니다.", 400),
    CATEGORY_NOT_FOUND("P003", "카테고리를 찾을 수 없습니다.", 400),
    TAG_NOT_FOUND("P004", "태그를 찾을 수 없습니다.", 400),
    INVALID_CATEGORY_TYPE("P005", "프롬프트 생성에는 소분류(SCS) 카테고리를 선택해야 합니다.", 400),
    GEMINI_EMPTY_RESPONSE("P006", "Gemini API로부터 유효한 응답을 받지 못했습니다.", 500),
    HISTORY_NOT_FOUND("P007", "프롬프트 이력을 찾을 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

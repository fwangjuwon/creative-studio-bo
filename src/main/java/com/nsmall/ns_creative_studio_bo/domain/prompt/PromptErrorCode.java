package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Prompt 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@RequiredArgsConstructor
public enum PromptErrorCode implements ErrorCode {

    DAILY_LIMIT_EXCEEDED("P001", "일일 프롬프트 생성 한도를 초과했습니다.", HttpStatus.BAD_REQUEST.value()),
    USER_NOT_FOUND("P002", "사용자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    CATEGORY_NOT_FOUND("P003", "카테고리를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    TAG_NOT_FOUND("P004", "태그를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_CATEGORY_TYPE("P005", "프롬프트 생성에는 소분류(SCS) 카테고리를 선택해야 합니다.", HttpStatus.BAD_REQUEST.value()),
    GEMINI_EMPTY_RESPONSE("P006", "Gemini API로부터 유효한 응답을 받지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    HISTORY_NOT_FOUND("P007", "프롬프트 이력을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    GEMINI_API_ERROR("P008", "Gemini API 호출 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    GEMINI_RATE_LIMITED("P009", "프롬프트 생성 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    GEMINI_AUTH_FAILED("P010", "프롬프트 생성 서비스 인증에 실패했습니다. 관리자에게 문의해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    GEMINI_UNAVAILABLE("P011", "프롬프트 생성 서비스가 일시적으로 이용 불가합니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}

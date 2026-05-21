package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인간 이미지 프롬프트 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@RequiredArgsConstructor
public enum HumanPromptErrorCode implements ErrorCode {

    NO_OPTIONS_SELECTED("HP001", "선택된 옵션이 없습니다.", HttpStatus.BAD_REQUEST.value()),
    GEMINI_EMPTY_RESPONSE("HP002", "Gemini API로부터 유효한 응답을 받지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    REQUIRED_OPTION_MISSING("HP003", "필수 항목이 선택되지 않았습니다.", HttpStatus.BAD_REQUEST.value()),

    STEP_NOT_FOUND("HP010", "존재하지 않는 단계입니다.", HttpStatus.BAD_REQUEST.value()),
    STEP_HAS_ATTRS("HP011", "하위 세부항목이 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    ATTR_NOT_FOUND("HP012", "존재하지 않는 세부항목입니다.", HttpStatus.BAD_REQUEST.value()),
    ATTR_HAS_OPTIONS("HP013", "하위 선택지가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    ATTR_LAST_REQUIRED("HP020", "단계에 필수 세부항목이 최소 1개 이상 있어야 합니다.", HttpStatus.BAD_REQUEST.value()),

    OPTION_NOT_FOUND("HP014", "존재하지 않는 선택지입니다.", HttpStatus.BAD_REQUEST.value()),
    OPTION_IN_USE("HP015", "프롬프트 이력에서 사용 중인 선택지는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    DUPLICATE_STEP_NAME("HP016", "이미 존재하는 단계명입니다.", HttpStatus.BAD_REQUEST.value()),
    DUPLICATE_STEP_CODE("HP017", "이미 존재하는 단계 코드입니다.", HttpStatus.BAD_REQUEST.value()),
    DUPLICATE_STEP_NUMBER("HP021", "이미 존재하는 순서 번호입니다.", HttpStatus.BAD_REQUEST.value()),
    DUPLICATE_ATTR_NAME("HP018", "이미 존재하는 세부항목명입니다.", HttpStatus.BAD_REQUEST.value()),
    DUPLICATE_OPTION_NAME("HP019", "이미 존재하는 선택지명입니다.", HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}

package com.nsmall.ns_creative_studio_bo.domain.user;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * User 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    DUPLICATE_LOGIN_ID("U001", "이미 사용 중인 로그인 아이디입니다.", HttpStatus.BAD_REQUEST.value()),
    USER_NOT_FOUND("U002", "회원을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    USER_BANNED("U003", "비활성화된 계정입니다.", HttpStatus.UNAUTHORIZED.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}

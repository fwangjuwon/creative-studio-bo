package com.nsmall.ns_creative_studio_bo.domain.user;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * User 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
/**
 * User 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    DUPLICATE_LOGIN_ID("U001", "이미 사용 중인 로그인 아이디입니다.", 400),
    USER_NOT_FOUND("U002", "회원을 찾을 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

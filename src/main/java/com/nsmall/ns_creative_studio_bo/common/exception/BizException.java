package com.nsmall.ns_creative_studio_bo.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외 클래스
 *
 * <pre>
 * 비즈니스 로직에서 발생하는 예외를 처리하기 위한 클래스
 * ErrorCode enum을 사용하여 타입 안전한 에러 처리 지원
 * HTTP 상태 코드를 포함하여 GlobalExceptionHandler에서 동적으로 처리
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode enum을 사용한 생성자 (권장)
     *
     * @param errorCode 에러 코드 enum
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode enum과 커스텀 메시지를 사용한 생성자
     *
     * @param errorCode     에러 코드 enum
     * @param customMessage 커스텀 에러 메시지
     */
    public BizException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode enum과 원인 예외를 사용한 생성자
     *
     * @param errorCode 에러 코드 enum
     * @param cause     원인 예외
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}

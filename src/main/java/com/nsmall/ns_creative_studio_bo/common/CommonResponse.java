package com.nsmall.ns_creative_studio_bo.common;

import org.springframework.http.HttpStatus;

/**
 * API 불변 공통 응답 래퍼
 *
 * <pre>
 * 모든 API 응답은 이 record를 통해 일관된 형식으로 반환한다.
 * 성공: success(), 클라이언트 오류: fail(), 서버 오류: error()
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
public record CommonResponse<T>(int code, String message, T data) {

    // -------------------------------------------------------------------------
    // Success
    // -------------------------------------------------------------------------

    public static <T> CommonResponse<T> success(int code, String message, T data) {
        return new CommonResponse<>(code, message, data);
    }

    /** @return 200, success, data */
    public static <T> CommonResponse<T> success(T data) {
        return success(HttpStatus.OK.value(), "success", data);
    }

    /** @return 200, success, null */
    public static <T> CommonResponse<T> success() {
        return success(null);
    }

    // -------------------------------------------------------------------------
    // Fail (4xx)
    // -------------------------------------------------------------------------

    /**
     * 클라이언트 에러 응답 (4xx)
     *
     * @param code    HTTP 상태 코드
     * @param message 에러 메시지
     */
    public static <T> CommonResponse<T> fail(int code, String message) {
        return new CommonResponse<>(code, message, null);
    }

    /**
     * 클라이언트 에러 응답 (400 Bad Request)
     *
     * @param message 에러 메시지
     */
    public static <T> CommonResponse<T> fail(String message) {
        return fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    // -------------------------------------------------------------------------
    // Error (5xx)
    // -------------------------------------------------------------------------

    /**
     * 서버 에러 응답 (5xx)
     *
     * @param code    HTTP 상태 코드
     * @param message 에러 메시지
     */
    public static <T> CommonResponse<T> error(int code, String message) {
        return new CommonResponse<>(code, message, null);
    }

    /**
     * 서버 에러 응답 (500 Internal Server Error)
     *
     * @param message 에러 메시지
     */
    public static <T> CommonResponse<T> error(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
}
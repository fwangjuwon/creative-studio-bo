package com.nsmall.ns_creative_studio_bo.common.exception;

/**
 * 에러 코드 인터페이스
 *
 * <pre>
 * 각 도메인별로 ErrorCode를 구현하여 독립적인 에러 코드 관리
 *
 * REST API 응답을 위한 에러 코드 정의
 * HttpStatus는 REST API 컨텍스트에서만 유효
 * REST API 응답을 위한 HTTP 상태 코드 포함
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
public interface ErrorCode {

    /** @return 에러 코드 (예: "CMN001", "BL001", "GDS001") */
    String getCode();

    /** @return 에러 메시지 */
    String getMessage();

    /** @return HTTP 상태 코드 (예: 400, 401, 404, 500) */
    int getHttpStatus();
}

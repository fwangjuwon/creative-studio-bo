package com.nsmall.ns_creative_studio_bo.common.exception;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 글로벌 예외 핸들러
 *
 * <pre>
 * 모든 컨트롤러에서 발생하는 예외를 일관된 {@link CommonResponse} 형식으로 변환
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 인증 실패 (토큰 없음 / 만료 / 위조) — 401 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<Void>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        log.warn("인증 실패 [API: {} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.fail(HttpStatus.UNAUTHORIZED.value(), "인증이 필요합니다."));
    }

    /** 권한 부족 — 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Void>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        log.warn("접근 거부 [API: {} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonResponse.fail(HttpStatus.FORBIDDEN.value(), "접근 권한이 없습니다."));
    }

    /** 비즈니스 예외 처리 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<CommonResponse<Void>> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("비즈니스 예외 발생 [코드: {}] [HTTP: {}] [API: {} {}] [파라미터: {}]: {}",
                e.getErrorCode().getCode(),
                e.getHttpStatus(),
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e.getMessage(),
                e);

        int status = e.getHttpStatus();
        return ResponseEntity
                .status(status)
                .body((status >= 500)
                        ? CommonResponse.error(status, e.getMessage())
                        : CommonResponse.fail(status, e.getMessage()));
    }

    /** DTO Validation 실패 (@Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation 예외 [API: {} {}] [파라미터: {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                errorMessage,
                e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail(HttpStatus.BAD_REQUEST.value(), errorMessage));
    }

    /** 메서드 파라미터 Validation 실패 (@Validated + @PathVariable/@RequestParam) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("제약조건 위반 [API: {} {}] [파라미터: {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                errorMessage,
                e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail(HttpStatus.BAD_REQUEST.value(), errorMessage));
    }

    /** DB 관련 예외 */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<CommonResponse<Void>> handleDataAccessException(
            DataAccessException e, HttpServletRequest request) {
        log.error("DB 오류 발생 [API: {} {}] [파라미터: {}]",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "데이터베이스 오류가 발생했습니다"));
    }

    /** MyBatis 시스템 예외 */
    @ExceptionHandler(MyBatisSystemException.class)
    public ResponseEntity<CommonResponse<Void>> handleMyBatisSystemException(
            MyBatisSystemException e, HttpServletRequest request) {
        log.error("MyBatis 오류 발생 [API: {} {}] [파라미터: {}]",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "데이터베이스 오류가 발생했습니다"));
    }

    /** JSON 파싱 실패 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("잘못된 요청 형식 [API: {} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage(),
                e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail(HttpStatus.BAD_REQUEST.value(), "잘못된 요청 형식입니다"));
    }

    /** 지원하지 않는 HTTP 메서드 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("지원하지 않는 HTTP 메서드 [API: {} {}] [파라미터: {}]",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e);
        String message = "지원하지 않는 요청입니다. 허용 메서드: "
                + (e.getSupportedHttpMethods() != null
                        ? e.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))
                        : "없음");
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(CommonResponse.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), message));
    }

    /** Path/QueryParam 타입 변환 실패 (@PathVariable, @RequestParam → Integer 등) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("'%s' 파라미터 값이 올바르지 않습니다: %s", e.getName(), e.getValue());
        log.warn("파라미터 타입 변환 실패 [API: {} {}] [파라미터: {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                message,
                e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail(HttpStatus.BAD_REQUEST.value(), message));
    }

    /** 필수 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("필수 파라미터 누락 [API: {} {}] [파라미터: {}] [누락: {}]",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e.getParameterName(),
                e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail(HttpStatus.BAD_REQUEST.value(), "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    /** 리소스를 찾을 수 없음 (존재하지 않는 API 경로 또는 정적 리소스) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFound(
            NoResourceFoundException e, HttpServletRequest request) {
        log.warn("리소스를 찾을 수 없음 [API: {} {}] [리소스: {}]",
                request.getMethod(),
                request.getRequestURI(),
                e.getResourcePath(),
                e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.fail(HttpStatus.NOT_FOUND.value(), "요청한 리소스를 찾을 수 없습니다"));
    }

    /** 예상하지 못한 예외 (최종 방어선) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("예상하지 못한 예외 발생 [API: {} {}] [파라미터: {}]",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다"));
    }
}

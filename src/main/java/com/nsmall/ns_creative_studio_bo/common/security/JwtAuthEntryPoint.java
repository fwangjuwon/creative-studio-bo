package com.nsmall.ns_creative_studio_bo.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 진입점 — 미인증 요청에 대해 {@link CommonResponse} 형식으로 401 반환
 *
 * <pre>
 * 호출 시점: 토큰이 없거나 만료된 상태로 보호된 리소스에 접근할 때
 * Spring Security의 ExceptionTranslationFilter에 의해 호출되므로
 * GlobalExceptionHandler가 아닌 이 클래스에서 응답을 직접 작성한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("미인증 접근 [API: {} {}]: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonResponse<Void> body = CommonResponse.fail(HttpStatus.UNAUTHORIZED.value(), "인증이 필요합니다.");
        objectMapper.writeValue(response.getWriter(), body);
    }
}

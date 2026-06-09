package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * GeminiServiceImpl — Gemini API 에러 코드 분기 단위 테스트
 *
 * <pre>
 * RestClient를 모킹하여 Gemini API가 반환하는 HTTP 상태 코드별로
 * 올바른 PromptErrorCode로 변환되는지 검증한다.
 * 상품/인물 프롬프트 생성 모두 동일한 resolvePromptErrorCode()를 사용하므로
 * generateGoodsPrompt()로 대표 검증한다.
 * DB 불필요 — MockitoExtension 단독 실행.
 * </pre>
 *
 * @author iamjy
 * @since 2026-05-04
 */
@ExtendWith(MockitoExtension.class)
public class GeminiServiceImplTest {

    @Mock
    RestClient geminiRestClient;

    @Mock
    GeminiProperties geminiProperties;

    @Mock
    RestClient.RequestBodyUriSpec requestSpec;

    @Mock
    RestClient.ResponseSpec responseSpec;

    @InjectMocks
    GeminiServiceImpl geminiService;

    @BeforeEach
    void setUp() {
        given(geminiProperties.getModel()).willReturn("gemini-3-flash-preview");
        given(geminiRestClient.post()).willReturn(requestSpec);
        given(requestSpec.uri(anyString())).willReturn(requestSpec);
        given(requestSpec.contentType(any(MediaType.class))).willReturn(requestSpec);
        given(requestSpec.body(any(Object.class))).willReturn(requestSpec);
        given(requestSpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("[P009] 429 RESOURCE_EXHAUSTED → GEMINI_RATE_LIMITED")
    void 예외_429_GEMINI_RATE_LIMITED() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_RATE_LIMITED));
    }

    @Test
    @DisplayName("[P010] 403 PERMISSION_DENIED → GEMINI_AUTH_FAILED")
    void 예외_403_GEMINI_AUTH_FAILED() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN, "Forbidden",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_AUTH_FAILED));
    }

    @Test
    @DisplayName("[P011] 500 INTERNAL → GEMINI_UNAVAILABLE")
    void 예외_500_GEMINI_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_UNAVAILABLE));
    }

    @Test
    @DisplayName("[P011] 503 UNAVAILABLE → GEMINI_UNAVAILABLE")
    void 예외_503_GEMINI_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_UNAVAILABLE));
    }

    @Test
    @DisplayName("[P011] 504 DEADLINE_EXCEEDED → GEMINI_UNAVAILABLE")
    void 예외_504_GEMINI_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_UNAVAILABLE));
    }

    @Test
    @DisplayName("[P008] 네트워크/타임아웃 에러 → GEMINI_API_ERROR")
    void 예외_네트워크_에러_GEMINI_API_ERROR() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> geminiService.generateGoodsPrompt(
                "인삼", List.of(), null, null, null, PromptStyle.NANO_BANANA))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.GEMINI_API_ERROR));
    }
}

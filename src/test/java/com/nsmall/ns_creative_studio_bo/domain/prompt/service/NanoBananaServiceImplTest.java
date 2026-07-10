package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.config.GeminiProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
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
import org.springframework.test.util.ReflectionTestUtils;
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
 * NanoBananaServiceImpl — Gemini API 에러 코드 분기 단위 테스트
 *
 * <pre>
 * RestClient를 모킹하여 Gemini API가 반환하는 HTTP 상태 코드별로
 * 올바른 ImageGenerationErrorCode로 변환되는지 검증한다.
 * DB 불필요 — MockitoExtension 단독 실행.
 * </pre>
 *
 * @author iamjy
 * @since 2026-05-04
 */
@ExtendWith(MockitoExtension.class)
public class NanoBananaServiceImplTest {

    @Mock
    RestClient geminiRestClient;

    @Mock
    GeminiProperties geminiProperties;

    @Mock
    RestClient.RequestBodyUriSpec requestSpec;

    @Mock
    RestClient.ResponseSpec responseSpec;

    @InjectMocks
    NanoBananaServiceImpl nanoBananaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(nanoBananaService, "storagePath", "/tmp/test-images");
        given(geminiProperties.getImageModel()).willReturn("gemini-3-pro-image-preview");
        given(geminiRestClient.post()).willReturn(requestSpec);
        given(requestSpec.uri(anyString())).willReturn(requestSpec);
        given(requestSpec.contentType(any(MediaType.class))).willReturn(requestSpec);
        given(requestSpec.body(any(Object.class))).willReturn(requestSpec);
        given(requestSpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("[IG007] 429 RESOURCE_EXHAUSTED → IMAGE_GENERATION_RATE_LIMITED")
    void 예외_429_IMAGE_GENERATION_RATE_LIMITED() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_RATE_LIMITED));
    }

    @Test
    @DisplayName("[IG008] 403 PERMISSION_DENIED → IMAGE_GENERATION_AUTH_FAILED")
    void 예외_403_IMAGE_GENERATION_AUTH_FAILED() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN, "Forbidden",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_AUTH_FAILED));
    }

    @Test
    @DisplayName("[IG009] 500 INTERNAL → IMAGE_GENERATION_UNAVAILABLE")
    void 예외_500_IMAGE_GENERATION_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_UNAVAILABLE));
    }

    @Test
    @DisplayName("[IG009] 503 UNAVAILABLE → IMAGE_GENERATION_UNAVAILABLE")
    void 예외_503_IMAGE_GENERATION_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_UNAVAILABLE));
    }

    @Test
    @DisplayName("[IG009] 504 DEADLINE_EXCEEDED → IMAGE_GENERATION_UNAVAILABLE")
    void 예외_504_IMAGE_GENERATION_UNAVAILABLE() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(HttpServerErrorException.create(
                        HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout",
                        new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_UNAVAILABLE));
    }

    @Test
    @DisplayName("[IG003] 네트워크/타임아웃 에러 → IMAGE_GENERATION_FAILED")
    void 예외_네트워크_에러_IMAGE_GENERATION_FAILED() {
        given(responseSpec.body(any(Class.class)))
                .willThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> nanoBananaService.generateImageMime("prompt", List.of(new ValidatedImage(new byte[0], "image/jpeg")), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED));
    }
}

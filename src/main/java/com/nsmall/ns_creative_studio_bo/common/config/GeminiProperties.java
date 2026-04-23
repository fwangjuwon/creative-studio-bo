package com.nsmall.ns_creative_studio_bo.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini API 설정 프로퍼티 — {@code application.yml} gemini.* 바인딩
 *
 * <pre>
 * 환경별로 달라지는 값(apiKey, model, baseUrl)만 관리한다.
 * 시스템 프롬프트는 비즈니스 로직의 일부이므로 GeminiServiceImpl 상수로 관리한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Component
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;
    private String model;
    private String baseUrl;

    /** 이미지 생성 전용 모델 (NanoBanana) — gemini-3-pro-image-preview */
    private String imageModel;
}

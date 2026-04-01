package com.nsmall.ns_creative_studio_bo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Gemini API RestClient Bean 설정
 *
 * <pre>
 * Spring RestClient를 사용하여 Gemini REST API와 통신.
 * API Key는 쿼리 파라미터 대신 x-goog-api-key 헤더로 전달하여
 * 프록시·액세스 로그 등에 노출되지 않도록 한다.
 * 시스템 프롬프트 및 모델 설정은 {@link GeminiProperties} 참조.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Configuration
public class GeminiConfig {

    @Bean
    public RestClient geminiRestClient(@Value("${gemini.base-url}") String baseUrl,
                                       GeminiProperties geminiProperties) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", geminiProperties.getApiKey())
                .build();
    }
}

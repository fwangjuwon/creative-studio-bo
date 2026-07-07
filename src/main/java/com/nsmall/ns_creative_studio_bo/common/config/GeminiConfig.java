package com.nsmall.ns_creative_studio_bo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

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
        String apiKey = Objects.requireNonNull(geminiProperties.getApiKey(),
                "GEMINI_API_KEY가 설정되지 않았습니다. .env 파일을 확인하세요.").trim();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 비어 있습니다. .env 파일을 확인하세요.");
        }
        // HTTP 소켓 타임아웃 설정 — orTimeout()은 Future만 취소하고 HTTP 스레드는 중단하지 않으므로
        // HTTP 레벨 타임아웃을 별도로 설정하여 스레드 풀 고갈 방지
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(150));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .requestFactory(requestFactory)
                .build();
    }
}

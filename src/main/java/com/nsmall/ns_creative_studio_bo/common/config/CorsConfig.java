package com.nsmall.ns_creative_studio_bo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 설정 — {@link CorsConfigurationSource} Bean 등록
 *
 * <pre>
 * local 프로필에서만 활성화된다.
 * dev/prod 환경은 APIM에서 CORS를 처리하므로 Spring CORS 설정을 비활성화한다.
 * SecurityConfig에서 {@code .cors(withDefaults())}로 참조된다.
 * 허용 Origin은 application.yml의 cors.allowed-origins로 관리한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-25
 */
@Profile("local")
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);  // Authorization 헤더(JWT) 전달 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

package com.nsmall.ns_creative_studio_bo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCryptPasswordEncoder Bean 설정
 *
 * <pre>
 * spring-security-crypto 모듈만 사용 (Spring Security 전체 스택 미사용)
 * Step 4에서 Spring Security 적용 시 SecurityConfig로 이전 예정
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

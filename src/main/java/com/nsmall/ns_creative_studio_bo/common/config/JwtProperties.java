package com.nsmall.ns_creative_studio_bo.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티 — {@code application.yml}의 {@code jwt.*} 바인딩
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 서명 키 (최소 32자 권장) */
    private String secret;

    /** Access Token 유효 시간 (단위: 초, 기본 3600 = 1시간) */
    private long expiration;
}

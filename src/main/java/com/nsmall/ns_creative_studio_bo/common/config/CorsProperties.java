package com.nsmall.ns_creative_studio_bo.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS 설정 프로퍼티 — {@code application.yml}의 {@code cors.*} 바인딩
 *
 * @author jjhoo8936
 * @since 2026-03-25
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 허용할 Origin 목록 (환경별 application-{profile}.yml에서 오버라이드) */
    private List<String> allowedOrigins;
}

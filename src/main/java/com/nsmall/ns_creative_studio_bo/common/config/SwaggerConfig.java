package com.nsmall.ns_creative_studio_bo.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Swagger(springdoc-openapi) 설정
 *
 * <pre>
 * Swagger UI: http://localhost:8080/swagger-ui/index.html
 * OpenAPI JSON: http://localhost:8080/v3/api-docs
 *
 * JWT Bearer 인증 스킴 등록 — "bearerAuth"
 * 각 Controller 메서드에서 @SecurityRequirement(name = "bearerAuth") 로 참조
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Profile({"local", "dev"})
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NS Creative Studio BO API")
                        .description("이미지 생성형 AI용 프롬프트 자동 생성 서비스 백엔드 API")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token — POST /api/auth/login 으로 발급")));
    }
}

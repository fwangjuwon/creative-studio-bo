package com.nsmall.ns_creative_studio_bo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 이미지 저장소 정적 리소스 핸들러 설정
 *
 * <pre>
 * NanaBanana 이미지 생성 결과를 서버 로컬 디렉토리에 저장하고
 * HTTP로 접근할 수 있도록 /images/** 경로를 정적 리소스로 매핑한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
@Configuration
public class ImageStorageConfig implements WebMvcConfigurer {

    @Value("${image.storage-path:./generated-images}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + storagePath + "/");
    }
}

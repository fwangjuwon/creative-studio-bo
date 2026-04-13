package com.nsmall.ns_creative_studio_bo.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * nsist SSO 연동 설정 프로퍼티 — {@code application-{profile}.yml}의 {@code nsist.*} 바인딩
 *
 * <pre>
 * referer-prefix : nsist에서 오는 요청의 Referer 헤더 허용 prefix (프로필별 URL 상이)
 * emp-cd-map     : nsist 사원코드(empCd) → NCS loginId 매핑 (임시 운영 방식, 추후 DB 컬럼으로 교체 예정)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-04-13
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nsist")
public class NsistProperties {

    /** nsist Referer 허용 prefix — 프로필별 application-{profile}.yml에서 설정 */
    private String refererPrefix;

    /** empCd → loginId 매핑 — 프로필별 application-{profile}.yml에서 설정 */
    private Map<String, String> empCdMap;
}

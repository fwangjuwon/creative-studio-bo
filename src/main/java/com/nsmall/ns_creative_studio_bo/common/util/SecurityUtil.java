package com.nsmall.ns_creative_studio_bo.common.util;

import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 컨텍스트에서 현재 인증된 사용자 정보를 추출하는 유틸리티
 *
 * <pre>
 * - 인증된 요청: 현재 로그인 사용자의 loginId 반환
 * - 미인증(테스트, 시스템 초기화 등): "system" 반환
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 현재 인증된 사용자의 loginId 반환.
     * SecurityContext에 인증 정보가 없으면 {@code "system"}을 반환한다.
     *
     * @return 현재 사용자 loginId 또는 "system"
     */
    public static String getCurrentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return "system";
    }
}

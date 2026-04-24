package com.nsmall.ns_creative_studio_bo.common;

import com.nsmall.ns_creative_studio_bo.common.config.SecurityConfig;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthEntryPoint;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * 컨트롤러 테스트 공통 기반 — Security 필터 설정 공유
 *
 * @author iamjy
 * @since 2026-04-24
 */
@Import(SecurityConfig.class)
@ActiveProfiles("local")
public abstract class SecuritySetupTest {

    @Autowired protected MockMvc mockMvc;

    @MockitoBean protected JwtAuthFilter jwtAuthFilter;
    @MockitoBean protected UserDetailsService userDetailsService;
    @MockitoBean protected JwtAuthEntryPoint jwtAuthEntryPoint;

    @BeforeEach
    void setUpSecurity() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            HttpServletResponse response = inv.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(jwtAuthEntryPoint).commence(any(), any(), any());
    }
}

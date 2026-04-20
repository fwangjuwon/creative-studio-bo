package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.config.SecurityConfig;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthEntryPoint;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthFilter;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth 컨트롤러 — Validation 실패 케이스
 *
 * @author jjhoo8936
 * @since 2026-04-16
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
public
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockitoBean AuthService authService;

    @BeforeEach
    void setUpJwtFilter() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private static final String LOGIN_URL = "/api/auth/login";

    // ============================================================
    // POST /api/auth/login — 로그인
    // ============================================================

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "[400] 로그인 - loginId 빈 값,              '',     pw123",
            "[400] 로그인 - loginId 공백만,              '   ', pw123",
            "[400] 로그인 - password 빈 값,              user1,  ''",
            "[400] 로그인 - loginId·password 모두 blank, '   ', '   '",
    })
    void 로그인_NotBlank_400(String displayName, String loginId, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("loginId", loginId, "password", password));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] 로그인 - Body 없음")
    void 로그인_Body없음_400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

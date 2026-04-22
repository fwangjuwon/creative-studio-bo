package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.config.SecurityConfig;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthEntryPoint;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthFilter;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-16
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
public class AuthControllerTest {

    @Autowired MockMvc mockMvc;

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

    @Test
    @DisplayName("[400] 로그인 - loginId 빈 값")
    void 로그인_loginId_빈값_400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "", "password": "pw123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] 로그인 - loginId 공백만")
    void 로그인_loginId_공백만_400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "   ", "password": "pw123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] 로그인 - password 빈 값")
    void 로그인_password_빈값_400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] 로그인 - loginId·password 모두 blank")
    void 로그인_모두_blank_400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "   ", "password": "   "}
                                """))
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

package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-16
 */
@WebMvcTest(AuthController.class)
public class AuthControllerTest extends SecuritySetupTest {

    @MockitoBean AuthService authService;

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

    @Test
    @DisplayName("[400] 로그인 - loginId 51자 초과")
    void 로그인_loginId_길이초과_400() throws Exception {
        String loginId = "a".repeat(51);
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "%s", "password": "pw123"}
                                """.formatted(loginId)))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/auth/nsist-login — nsist SSO 로그인
    // ============================================================

    private static final String NSIST_LOGIN_URL = "/api/auth/nsist-login";

    @Test
    @DisplayName("[400] nsist SSO 로그인 - empCd 빈 값")
    void nsist_로그인_empCd_빈값_400() throws Exception {
        mockMvc.perform(get(NSIST_LOGIN_URL)
                        .param("empCd", "")
                        .param("orgCd", "ORG01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] nsist SSO 로그인 - empCd 공백만")
    void nsist_로그인_empCd_공백만_400() throws Exception {
        mockMvc.perform(get(NSIST_LOGIN_URL)
                        .param("empCd", "   ")
                        .param("orgCd", "ORG01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] nsist SSO 로그인 - empCd 7자 초과")
    void nsist_로그인_empCd_길이초과_400() throws Exception {
        mockMvc.perform(get(NSIST_LOGIN_URL)
                        .param("empCd", "1234567")
                        .param("orgCd", "ORG01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[400] nsist SSO 로그인 - orgCd 빈 값")
    void nsist_로그인_orgCd_빈값_400() throws Exception {
        mockMvc.perform(get(NSIST_LOGIN_URL)
                        .param("empCd", "EMP01")
                        .param("orgCd", ""))
                .andExpect(status().isBadRequest());
    }
}

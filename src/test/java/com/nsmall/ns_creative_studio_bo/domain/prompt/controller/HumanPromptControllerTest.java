package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.config.SecurityConfig;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthEntryPoint;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthFilter;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptService;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptStepService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HumanPrompt 컨트롤러 — Validation 실패 케이스
 *
 * <pre>createPrompt는 {@code @AuthenticationPrincipal CustomUserDetails}를 사용하나,
 * Validation 400은 메서드 진입 전 발생하므로 {@code @WithMockUser}로 충분하다.</pre>
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(HumanPromptController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
public class HumanPromptControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockitoBean HumanPromptService humanPromptService;
    @MockitoBean HumanPromptStepService humanPromptStepService;

    @BeforeEach
    void setUpJwtFilter() throws Exception {
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

    private static final String BASE_URL = "/api/human-prompts";

    // ============================================================
    // POST /api/human-prompts — 인물 프롬프트 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 인물 프롬프트 생성 - selections null")
    void 인물프롬프트_생성_selections_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 인물 프롬프트 생성 - Body 없음")
    void 인물프롬프트_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // POST /api/human-prompts/steps — 단계 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 생성 - stepNumber null")
    void 단계_생성_stepNumber_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/steps")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepCode": "A", "name": "인물 유형"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 생성 - stepCode 빈 값")
    void 단계_생성_stepCode_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/steps")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "stepCode": "", "name": "인물 유형"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 생성 - name 빈 값")
    void 단계_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/steps")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "stepCode": "A", "name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 생성 - Body 없음")
    void 단계_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/steps")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/human-prompts/steps/{id} — 단계 수정
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 수정 - stepNumber null")
    void 단계_수정_stepNumber_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/steps/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "인물 유형"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 수정 - name 빈 값")
    void 단계_수정_name_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/steps/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 수정 - id가 양수가 아닌 경우")
    void 단계_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/steps/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "name": "인물 유형"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/human-prompts/steps/{id} — 단계 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 단계 삭제 - id가 양수가 아닌 경우")
    void 단계_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/steps/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // POST /api/human-prompts/generate-image — 인물 이미지 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 인물 이미지 생성 - promptText 빈 값")
    void 인물이미지_생성_promptText_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/generate-image")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"promptText": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 인물 이미지 생성 - Body 없음")
    void 인물이미지_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/generate-image")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 단계 생성 - USER 권한")
    void 단계_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/steps")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "stepCode": "A", "name": "인물 유형"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 단계 수정 - USER 권한")
    void 단계_수정_USER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/steps/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepNumber": 1, "name": "인물 유형"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 단계 삭제 - USER 권한")
    void 단계_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/steps/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 인물 프롬프트 생성 - 비로그인")
    void 인물프롬프트_생성_비로그인_401() throws Exception {
        mockMvc.perform(post(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.config.SecurityConfig;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthEntryPoint;
import com.nsmall.ns_creative_studio_bo.common.security.JwtAuthFilter;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptOptionService;
import jakarta.servlet.FilterChain;
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
 * HumanPromptOption 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(HumanPromptOptionController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
public class HumanPromptOptionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockitoBean HumanPromptOptionService humanPromptOptionService;

    @BeforeEach
    void setUpJwtFilter() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private static final String BASE_URL = "/api/human-prompts/options";

    // ============================================================
    // POST /api/human-prompts/options — 선택지 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 생성 - attrId null")
    void 선택지_생성_attrId_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "20대", "promptValue": "in their 20s", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 생성 - name 빈 값")
    void 선택지_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"attrId": 1, "name": "", "promptValue": "in their 20s", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 생성 - promptValue 빈 값")
    void 선택지_생성_promptValue_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"attrId": 1, "name": "20대", "promptValue": "", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 생성 - sortOrder null")
    void 선택지_생성_sortOrder_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"attrId": 1, "name": "20대", "promptValue": "in their 20s"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 생성 - Body 없음")
    void 선택지_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/human-prompts/options/{id} — 선택지 수정
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 수정 - name 빈 값")
    void 선택지_수정_name_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "promptValue": "in their 20s", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 수정 - promptValue 빈 값")
    void 선택지_수정_promptValue_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "20대", "promptValue": "", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 수정 - sortOrder null")
    void 선택지_수정_sortOrder_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "20대", "promptValue": "in their 20s"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 수정 - id가 양수가 아닌 경우")
    void 선택지_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "20대", "promptValue": "in their 20s", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/human-prompts/options/{id} — 선택지 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 선택지 삭제 - id가 양수가 아닌 경우")
    void 선택지_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 선택지 생성 - USER 권한")
    void 선택지_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"attrId": 1, "name": "20대", "promptValue": "in their 20s", "sortOrder": 1}
                                """))
                .andExpect(status().isForbidden());
    }
}

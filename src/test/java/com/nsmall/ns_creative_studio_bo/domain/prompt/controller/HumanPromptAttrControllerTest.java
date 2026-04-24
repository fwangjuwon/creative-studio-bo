package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptAttrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HumanPromptAttr 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(HumanPromptAttrController.class)
public class HumanPromptAttrControllerTest extends SecuritySetupTest {

    @MockitoBean HumanPromptAttrService humanPromptAttrService;

    private static final String BASE_URL = "/api/human-prompts/attrs";

    // ============================================================
    // POST /api/human-prompts/attrs — 세부항목 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - stepId null")
    void 세부항목_생성_stepId_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "연령대", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - name 빈 값")
    void 세부항목_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepId": 1, "name": "", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - isMultiSelect null")
    void 세부항목_생성_isMultiSelect_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepId": 1, "name": "연령대", "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - isRequired null")
    void 세부항목_생성_isRequired_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepId": 1, "name": "연령대", "isMultiSelect": false, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - sortOrder null")
    void 세부항목_생성_sortOrder_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepId": 1, "name": "연령대", "isMultiSelect": false, "isRequired": true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 생성 - Body 없음")
    void 세부항목_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/human-prompts/attrs/{id} — 세부항목 수정
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 수정 - name 빈 값")
    void 세부항목_수정_name_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 수정 - isMultiSelect null")
    void 세부항목_수정_isMultiSelect_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "연령대", "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 수정 - sortOrder null")
    void 세부항목_수정_sortOrder_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "연령대", "isMultiSelect": false, "isRequired": true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 수정 - id가 양수가 아닌 경우")
    void 세부항목_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "연령대", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/human-prompts/attrs/{id} — 세부항목 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 세부항목 삭제 - id가 양수가 아닌 경우")
    void 세부항목_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 세부항목 생성 - USER 권한")
    void 세부항목_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stepId": 1, "name": "연령대", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 세부항목 수정 - USER 권한")
    void 세부항목_수정_USER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "연령대", "isMultiSelect": false, "isRequired": true, "sortOrder": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 세부항목 삭제 - USER 권한")
    void 세부항목_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 세부항목 생성 - 비로그인")
    void 세부항목_생성_비로그인_401() throws Exception {
        mockMvc.perform(post(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}

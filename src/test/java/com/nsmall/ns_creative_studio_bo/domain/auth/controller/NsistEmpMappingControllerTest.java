package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.NsistEmpMappingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NsistEmpMapping 컨트롤러 — Validation 실패 케이스 및 권한 제어 검증
 *
 * @author jjhoo8936
 * @since 2026-05-13
 */
@WebMvcTest(NsistEmpMappingController.class)
public class NsistEmpMappingControllerTest extends SecuritySetupTest {

    @MockitoBean
    NsistEmpMappingService nsistEmpMappingService;

    private static final String BASE_URL = "/api/nsist-emp-mappings";

    // ============================================================
    // POST /api/nsist-emp-mappings — 매핑 등록
    // ============================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 등록 - empCd 빈 값")
    void 매핑_등록_empCd_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "", "userId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 등록 - empCd 공백만")
    void 매핑_등록_empCd_공백만_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "   ", "userId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 등록 - userId null")
    void 매핑_등록_userId_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "12345"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 등록 - empCd 7자 초과")
    void 매핑_등록_empCd_길이초과_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "1234567", "userId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 등록 - Body 없음")
    void 매핑_등록_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/nsist-emp-mappings/{empCd} — 매핑 삭제
    // ============================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 매핑 삭제 - empCd 7자 초과")
    void 매핑_삭제_empCd_길이초과_400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{empCd}", "1234567"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한 — POST / GET / DELETE
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 매핑 등록 - MANAGER 권한")
    void 매핑_등록_MANAGER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "12345", "userId": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 매핑 등록 - USER 권한")
    void 매핑_등록_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "12345", "userId": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 매핑 목록 조회 - MANAGER 권한")
    void 매핑_목록조회_MANAGER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 매핑 목록 조회 - USER 권한")
    void 매핑_목록조회_USER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 매핑 삭제 - MANAGER 권한")
    void 매핑_삭제_MANAGER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{empCd}", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 매핑 삭제 - USER 권한")
    void 매핑_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{empCd}", "12345"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 매핑 목록 조회 - 비로그인")
    void 매핑_목록조회_비로그인_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[401] 매핑 등록 - 비로그인")
    void 매핑_등록_비로그인_401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"empCd": "12345", "userId": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }
}

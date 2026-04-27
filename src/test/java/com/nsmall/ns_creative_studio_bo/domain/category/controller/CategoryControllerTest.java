package com.nsmall.ns_creative_studio_bo.domain.category.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.category.service.CategoryService;
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
 * Category 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(CategoryController.class)
public class CategoryControllerTest extends SecuritySetupTest {

    @MockitoBean CategoryService categoryService;

    private static final String BASE_URL = "/api/categories";

    // ============================================================
    // POST /api/categories — 카테고리 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 생성 - name 빈 값")
    void 카테고리_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "categoryType": "LCS"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 생성 - categoryType null")
    void 카테고리_생성_categoryType_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "식품"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 생성 - Body 없음")
    void 카테고리_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/categories/{id} — 카테고리 단건 조회
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 카테고리 단건 조회 - id가 양수가 아닌 경우")
    void 카테고리_단건조회_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/categories/{id} — 카테고리 수정
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 수정 - name 빈 값")
    void 카테고리_수정_name_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 수정 - sortOrder null")
    void 카테고리_수정_sortOrder_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "식품"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 수정 - id가 양수가 아닌 경우")
    void 카테고리_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "식품", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/categories/{id} — 카테고리 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 카테고리 삭제 - id가 양수가 아닌 경우")
    void 카테고리_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 카테고리 생성 - USER 권한")
    void 카테고리_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "식품", "categoryType": "LCS"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 카테고리 수정 - USER 권한")
    void 카테고리_수정_USER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "식품", "sortOrder": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 카테고리 삭제 - USER 권한")
    void 카테고리_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 카테고리 목록 조회 - 비로그인")
    void 카테고리_목록조회_비로그인_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}

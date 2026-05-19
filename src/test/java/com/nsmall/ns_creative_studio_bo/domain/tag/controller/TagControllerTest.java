package com.nsmall.ns_creative_studio_bo.domain.tag.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagService;
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
 * Tag 컨트롤러 — Validation 실패 케이스
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(TagController.class)
public class TagControllerTest extends SecuritySetupTest {

    @MockitoBean TagService tagService;

    private static final String BASE_URL = "/api/tags";

    // ============================================================
    // POST /api/tags — 태그 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 생성 - tagGroupId null")
    void 태그_생성_tagGroupId_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "남자"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 생성 - name 빈 값")
    void 태그_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"tagGroupId": 1, "name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 생성 - name 101자 초과")
    void 태그_생성_name_길이초과_400() throws Exception {
        String name = "가".repeat(101);
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"tagGroupId": 1, "name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 생성 - Body 없음")
    void 태그_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/tags/{id} — 태그 단건 조회
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 태그 단건 조회 - id가 양수가 아닌 경우")
    void 태그_단건조회_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/tags/{id} — 태그 수정
    // ============================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 수정 - name 빈 값")
    void 태그_수정_name_빈값_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 수정 - name 101자 초과")
    void 태그_수정_name_길이초과_400() throws Exception {
        String name = "가".repeat(101);
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "sortOrder": 1}
                                """.formatted(name)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 수정 - sortOrder null")
    void 태그_수정_sortOrder_null_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "남자"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 수정 - id가 양수가 아닌 경우")
    void 태그_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "남자", "sortOrder": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/tags/{id} — 태그 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[400] 태그 삭제 - id가 양수가 아닌 경우")
    void 태그_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 태그 생성 - USER 권한")
    void 태그_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"tagGroupId": 1, "name": "남자"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 태그 수정 - USER 권한")
    void 태그_수정_USER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "남자", "sortOrder": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 태그 삭제 - USER 권한")
    void 태그_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 태그 목록 조회 - 비로그인")
    void 태그_목록조회_비로그인_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}

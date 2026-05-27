package com.nsmall.ns_creative_studio_bo.domain.user.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User 컨트롤러 — Validation / 접근 권한 케이스
 *
 * @author iamjy
 * @since 2026-04-17
 */
@WebMvcTest(UserController.class)
public class UserControllerTest extends SecuritySetupTest {

    @MockitoBean UserService userService;

    private static final String BASE_URL = "/api/users";

    /** 역할(role)별 CustomUserDetails 생성 헬퍼 */
    private CustomUserDetails customUserDetails(String role) {
        User user = User.builder().id(1L).loginId("testuser").role(role).useYn("Y").build();
        return new CustomUserDetails(user);
    }

    // ============================================================
    // POST /api/users — 사용자 생성
    // ============================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - loginId 빈 값")
    void 사용자_생성_loginId_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "", "password": "pass123", "name": "홍길동", "role": "ADMIN", "dailyLimit": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - password 빈 값")
    void 사용자_생성_password_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "", "name": "홍길동", "role": "ADMIN", "dailyLimit": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - name 빈 값")
    void 사용자_생성_name_빈값_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "", "role": "ADMIN", "dailyLimit": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GUEST", "admin"})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - role 패턴 위반")
    void 사용자_생성_role_패턴위반_400(String role) throws Exception {
        String body = """
                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "%s", "dailyLimit": 10}
                """.formatted(role);
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - dailyLimit null")
    void 사용자_생성_dailyLimit_null_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "ADMIN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - dailyLimit=-1")
    void 사용자_생성_dailyLimit_음수_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "ADMIN", "dailyLimit": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - loginId 51자 초과")
    void 사용자_생성_loginId_길이초과_400() throws Exception {
        String loginId = "a".repeat(51);
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "%s", "password": "pass123", "name": "홍길동", "role": "ADMIN", "dailyLimit": 10}
                                """.formatted(loginId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - teamName 101자 초과")
    void 사용자_생성_teamName_길이초과_400() throws Exception {
        String teamName = "가".repeat(101);
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "ADMIN", "teamName": "%s", "dailyLimit": 10}
                                """.formatted(teamName)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - name 101자 초과")
    void 사용자_생성_name_길이초과_400() throws Exception {
        String name = "가".repeat(101);
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "%s", "role": "ADMIN", "dailyLimit": 10}
                                """.formatted(name)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 생성 - Body 없음")
    void 사용자_생성_Body없음_400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/users/{id} — 사용자 단건 조회
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 단건 조회 - id가 양수가 아닌 경우")
    void 사용자_단건조회_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // PUT /api/users/{id} — 사용자 수정
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 수정 - id가 양수가 아닌 경우")
    void 사용자_수정_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "홍길동"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 수정 - role=GUEST")
    void 사용자_수정_role_패턴위반_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"role": "GUEST"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 수정 - dailyLimit=-1")
    void 사용자_수정_dailyLimit_음수_400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dailyLimit": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 수정 - name 101자 초과")
    void 사용자_수정_name_길이초과_400() throws Exception {
        String name = "가".repeat(101);
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 수정 - teamName 101자 초과")
    void 사용자_수정_teamName_길이초과_400() throws Exception {
        String teamName = "가".repeat(101);
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"teamName": "%s"}
                                """.formatted(teamName)))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // DELETE /api/users/{id} — 사용자 삭제
    // ============================================================

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -999L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[400] 사용자 삭제 - id가 양수가 아닌 경우")
    void 사용자_삭제_id_양수아님_400(long id) throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // GET /api/users/me/usage — 내 사용량 조회
    // ============================================================

    @Test
    @DisplayName("[200] 내 사용량 조회 - USER 권한 허용")
    void 내사용량_조회_USER권한_200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me/usage")
                        .with(user(customUserDetails("USER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[200] 내 사용량 조회 - MANAGER 권한 허용")
    void 내사용량_조회_MANAGER권한_200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me/usage")
                        .with(user(customUserDetails("MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[200] 내 사용량 조회 - ADMIN 권한 허용")
    void 내사용량_조회_ADMIN권한_200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me/usage")
                        .with(user(customUserDetails("ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[401] 내 사용량 조회 - 비로그인")
    void 내사용량_조회_비로그인_401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me/usage"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // 403 권한
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 사용자 생성 - USER 권한")
    void 사용자_생성_USER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "USER", "dailyLimit": 10}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 사용자 생성 - MANAGER 권한")
    void 사용자_생성_MANAGER권한_403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"loginId": "user1", "password": "pass123", "name": "홍길동", "role": "ADMIN", "dailyLimit": 10}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 사용자 단건 조회 - USER 권한")
    void 사용자_단건조회_USER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 사용자 단건 조회 - MANAGER 권한")
    void 사용자_단건조회_MANAGER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 사용자 목록 조회 - USER 권한")
    void 사용자_목록조회_USER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 사용자 목록 조회 - MANAGER 권한")
    void 사용자_목록조회_MANAGER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 사용자 수정 - USER 권한")
    void 사용자_수정_USER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "홍길동"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 사용자 수정 - MANAGER 권한")
    void 사용자_수정_MANAGER권한_403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "홍길동"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 사용자 삭제 - USER 권한")
    void 사용자_삭제_USER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 사용자 삭제 - MANAGER 권한")
    void 사용자_삭제_MANAGER권한_403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 특정 회원 사용량 조회 - USER 권한")
    void 특정회원_사용량조회_USER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}/usage", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 특정 회원 사용량 조회 - MANAGER 권한")
    void 특정회원_사용량조회_MANAGER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}/usage", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[403] 전체 회원 사용량 목록 조회 - USER 권한")
    void 전체회원_사용량목록조회_USER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/usage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("[403] 전체 회원 사용량 목록 조회 - MANAGER 권한")
    void 전체회원_사용량목록조회_MANAGER권한_403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/usage"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 사용자 목록 조회 - 비로그인")
    void 사용자_목록조회_비로그인_401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}

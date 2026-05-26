package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.FusionImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FusionImage 컨트롤러 — 인증/인가 및 필수 파라미터 검증 케이스
 *
 * @author gaeddo
 * @since 2026-05-26
 */
@WebMvcTest(FusionImageController.class)
public class FusionImageControllerTest extends SecuritySetupTest {

    @MockitoBean FusionImageService fusionImageService;

    private static final String BASE_URL = "/api/prompts/fusion/generate-image";

    private static final MockMultipartFile PRODUCT_IMAGE =
            new MockMultipartFile("productImage", "product.jpg", "image/jpeg", new byte[]{1, 2, 3});
    private static final MockMultipartFile PERSON_IMAGE =
            new MockMultipartFile("personImage", "person.jpg", "image/jpeg", new byte[]{1, 2, 3});

    // ============================================================
    // 400 필수 파라미터 누락
    // ============================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 합성 이미지 생성 - productImage 미전송")
    void 합성이미지_생성_productImage_미전송_400() throws Exception {
        mockMvc.perform(multipart(BASE_URL)
                        .file(PERSON_IMAGE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 합성 이미지 생성 - personImage 미전송")
    void 합성이미지_생성_personImage_미전송_400() throws Exception {
        mockMvc.perform(multipart(BASE_URL)
                        .file(PRODUCT_IMAGE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[400] 합성 이미지 생성 - 이미지 전체 미전송")
    void 합성이미지_생성_이미지_전체_미전송_400() throws Exception {
        mockMvc.perform(multipart(BASE_URL))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 401 비로그인
    // ============================================================

    @Test
    @DisplayName("[401] 합성 이미지 생성 - 비로그인")
    void 합성이미지_생성_비로그인_401() throws Exception {
        mockMvc.perform(multipart(BASE_URL)
                        .file(PRODUCT_IMAGE)
                        .file(PERSON_IMAGE))
                .andExpect(status().isUnauthorized());
    }
}

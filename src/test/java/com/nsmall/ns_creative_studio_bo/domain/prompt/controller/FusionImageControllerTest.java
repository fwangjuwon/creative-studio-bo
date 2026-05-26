package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.SecuritySetupTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.FusionImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FusionImage 컨트롤러 — 인증 검증 케이스
 *
 * <pre>
 * 파일 누락(null/empty) 검증은 서비스 레이어(FusionImageServiceImpl)에서
 * FUSION_IMAGE_REQUIRED 에러코드로 처리하므로 컨트롤러 테스트에서는 다루지 않는다.
 * @RequestParam MultipartFile은 파트 누락 시 Spring이 null로 처리하여
 * 컨트롤러 레이어에서 400을 발생시키지 않기 때문.
 * </pre>
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

package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FusionImageService 통합 테스트 — 이미지 유효성 검증 + 합성 이미지 생성 흐름 검증
 *
 * <pre>
 * ImageGenerationService는 외부 API 의존성이므로 @MockitoBean으로 모킹하여 테스트.
 * 이미지 유효성 검증은 ImageValidationUtil 위임 — 별도 단위 테스트는 ImageValidationUtilTest 참고.
 * DB 접근 없음 — @Transactional/@Rollback 불필요.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-19
 */
@SpringBootTest
@ActiveProfiles("local")
public class FusionImageServiceTest {

    @Autowired
    private FusionImageService fusionImageService;

    @MockitoBean
    private ImageGenerationService imageGenerationService;

    private static final byte[] DUMMY_BYTES = new byte[]{1, 2, 3};

    private MockMultipartFile 유효한_이미지(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", DUMMY_BYTES);
    }

    private MockMultipartFile 크기_초과_이미지(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[16 * 1024 * 1024]);
    }

    private MockMultipartFile 미허용_MIME_이미지(String name, String mimeType) {
        return new MockMultipartFile(name, name + ".file", mimeType, DUMMY_BYTES);
    }

    // ================================================================
    // 합성 이미지 생성 — 성공
    // ================================================================

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - 정상 케이스")
    void 성공_합성_이미지_생성_정상() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt()))
                .willReturn(List.of("/images/fusion-1.jpg", "/images/fusion-2.jpg", "/images/fusion-3.jpg"));

        List<String> result = fusionImageService.generateFusionImages(
                유효한_이미지("productImage"),
                유효한_이미지("personImage"),
                "상품을 양손으로 들고 있는", "밝은 스튜디오 배경", "1:1"
        );

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(url -> url.startsWith("http"));
    }

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - pose/instruction/imageAspectRatio 모두 null")
    void 성공_합성_이미지_생성_선택_파라미터_null() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt()))
                .willReturn(List.of("/images/fusion-1.jpg"));

        List<String> result = fusionImageService.generateFusionImages(
                유효한_이미지("productImage"), 유효한_이미지("personImage"), null, null, null);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - userMessage에 pose/instruction/aspectRatio 포함 검증")
    @SuppressWarnings("unchecked")
    void 성공_합성_이미지_생성_userMessage_구성_검증() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt()))
                .willReturn(List.of("/images/fusion-1.jpg"));

        fusionImageService.generateFusionImages(
                유효한_이미지("productImage"), 유효한_이미지("personImage"),
                "상품을 들고 있는", "따뜻한 배경", "16:9");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageGenerationService).generateFusionImages(
                messageCaptor.capture(), any(), any(), any(), any(), eq(PromptConstants.IMAGE_GENERATE_COUNT));

        String captured = messageCaptor.getValue();
        assertThat(captured).contains("상품을 들고 있는");
        assertThat(captured).contains("따뜻한 배경");
        assertThat(captured).contains("16:9");
    }

    // ================================================================
    // 합성 이미지 생성 — 예외
    // ================================================================

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 상품 이미지 크기 초과")
    void 예외_합성_이미지_생성_상품_크기_초과() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                크기_초과_이미지("productImage"), 유효한_이미지("personImage"), null, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_TOO_LARGE));
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 인물 이미지 크기 초과")
    void 예외_합성_이미지_생성_인물_크기_초과() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                유효한_이미지("productImage"), 크기_초과_이미지("personImage"), null, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_TOO_LARGE));
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 지원하지 않는 MIME 타입")
    void 예외_합성_이미지_생성_미허용_MIME() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                미허용_MIME_이미지("productImage", "image/gif"), 유효한_이미지("personImage"), null, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }
}

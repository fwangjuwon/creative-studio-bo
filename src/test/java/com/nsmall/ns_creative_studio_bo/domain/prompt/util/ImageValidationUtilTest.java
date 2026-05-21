package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ImageValidationUtil 단위 테스트
 *
 * @author iamjy
 * @since 2026-05-20
 */
class ImageValidationUtilTest {

    @Test
    @DisplayName("[성공] 검증 - null 입력 → null 반환")
    void 성공_검증_null입력() {
        ValidatedImage result = ImageValidationUtil.validate(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("[성공] 검증 - empty 입력 → null 반환")
    void 성공_검증_empty입력() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(true);

        ValidatedImage result = ImageValidationUtil.validate(file);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("[성공] 검증 - 허용 MIME 타입")
    void 성공_검증_허용MIME타입(String mimeType) throws IOException {
        byte[] expectedBytes = new byte[]{1, 2, 3};
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn(mimeType);
        given(file.getBytes()).willReturn(expectedBytes);

        ValidatedImage result = ImageValidationUtil.validate(file);

        assertThat(result).isNotNull();
        assertThat(result.mimeType()).isEqualTo(mimeType);
        assertThat(result.bytes()).isEqualTo(expectedBytes);
    }

    @Test
    @DisplayName("[예외] 검증 - 파일 크기 15MB 초과")
    void 예외_검증_파일크기_초과() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(16 * 1024 * 1024L);

        assertThatThrownBy(() -> ImageValidationUtil.validate(file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_TOO_LARGE));
    }

    @Test
    @DisplayName("[예외] 검증 - Content-Type null")
    void 예외_검증_ContentType_null() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn(null);

        assertThatThrownBy(() -> ImageValidationUtil.validate(file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.INVALID_IMAGE_MIME_TYPE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/x-sh", "image/gif", "image/bmp"})
    @DisplayName("[예외] 검증 - 허용되지 않는 MIME 타입")
    void 예외_검증_허용되지않는_MIME타입(String mimeType) {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn(mimeType);

        assertThatThrownBy(() -> ImageValidationUtil.validate(file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }

    @Test
    @DisplayName("[예외] 검증 - getBytes IOException")
    void 예외_검증_getBytes_IOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.getBytes()).willThrow(new IOException("read failed"));

        assertThatThrownBy(() -> ImageValidationUtil.validate(file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_SAVE_FAILED));
    }
}

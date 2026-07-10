package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class ImageValidationUtilTest {

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
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_READ_FAILED));
    }

    @Test
    @DisplayName("[성공] 목록 검증 - null 입력 → 빈 리스트 반환")
    void 성공_목록검증_null입력() {
        List<ValidatedImage> result = ImageValidationUtil.validateList(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[성공] 목록 검증 - 빈 리스트 입력 → 빈 리스트 반환")
    void 성공_목록검증_빈리스트입력() {
        List<ValidatedImage> result = ImageValidationUtil.validateList(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[성공] 목록 검증 - 최대 개수 이내의 유효한 파일들 → 검증된 목록 반환")
    void 성공_목록검증_최대개수이내() throws IOException {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < PromptConstants.MAX_REFERENCE_IMAGES; i++) {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getSize()).willReturn(1024L);
            given(file.getContentType()).willReturn("image/jpeg");
            given(file.getBytes()).willReturn(new byte[]{1, 2, 3});
            files.add(file);
        }

        List<ValidatedImage> result = ImageValidationUtil.validateList(files);

        assertThat(result).hasSize(PromptConstants.MAX_REFERENCE_IMAGES);
    }

    @Test
    @DisplayName("[예외] 목록 검증 - 최대 개수 초과")
    void 예외_목록검증_최대개수초과() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < PromptConstants.MAX_REFERENCE_IMAGES + 1; i++) {
            files.add(mock(MultipartFile.class));
        }

        assertThatThrownBy(() -> ImageValidationUtil.validateList(files))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.TOO_MANY_REFERENCE_IMAGES));
    }

    @Test
    @DisplayName("[성공] 목록 검증 - empty 파일은 결과에서 제외")
    void 성공_목록검증_empty파일_제외() throws IOException {
        MultipartFile validFile = mock(MultipartFile.class);
        given(validFile.isEmpty()).willReturn(false);
        given(validFile.getSize()).willReturn(1024L);
        given(validFile.getContentType()).willReturn("image/jpeg");
        given(validFile.getBytes()).willReturn(new byte[]{1, 2, 3});

        MultipartFile emptyFile = mock(MultipartFile.class);
        given(emptyFile.isEmpty()).willReturn(true);

        List<ValidatedImage> result = ImageValidationUtil.validateList(List.of(validFile, emptyFile));

        assertThat(result).hasSize(1);
    }
}

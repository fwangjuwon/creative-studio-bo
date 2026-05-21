package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * 참조 이미지(MultipartFile) 검증 유틸 — 상품/인물 프롬프트 공용
 *
 * <pre>
 * 정책:
 *   - 허용 MIME: image/jpeg, image/png, image/webp
 *   - 최대 크기: 15MB
 *   - null/empty 입력은 "참조 이미지 없음"으로 간주하여 null 반환 (예외 아님)
 *
 * 설계 의도 — Spring Bean(@Component)으로 등록하지 않은 이유:
 *   - 상태(필드)와 외부 의존성(주입 대상)이 없는 순수 함수형 검증 로직
 *   - Spring 라이프사이클(init/destroy/AOP)이나 환경별 설정이 불필요
 *   - JDK Collections/Objects/Files 등과 동일한 정적 유틸 관용을 따름
 *   - 검증 규칙을 환경별로 교체하거나 외부 서비스(예: AV 스캔)를 주입해야 할 때
 *     비로소 빈으로 승격하는 것이 적절
 * </pre>
 *
 * @author iamjy
 * @since 2026-05-20
 */
@Slf4j
public final class ImageValidationUtil {

    public static final long MAX_BYTES = 15 * 1024 * 1024L;
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private ImageValidationUtil() {}

    /**
     * 참조 이미지 검증.
     *
     * @param file 검증할 MultipartFile (null 또는 empty 허용)
     * @return null/empty 입력 시 null, 그 외에는 검증 통과한 바이트와 MIME 타입
     * @throws BizException 크기 초과/누락 MIME/미허용 MIME/IO 실패
     */
    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(ImageGenerationErrorCode.IMAGE_TOO_LARGE);
        }
        String mimeType = file.getContentType();
        if (mimeType == null) {
            throw new BizException(ImageGenerationErrorCode.INVALID_IMAGE_MIME_TYPE);
        }
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            log.warn("허용되지 않은 MIME 타입 — mimeType={}", mimeType);
            throw new BizException(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        try {
            return new ValidatedImage(file.getBytes(), mimeType);
        } catch (IOException e) {
            log.error("참조 이미지 읽기 실패", e);
            throw new BizException(ImageGenerationErrorCode.IMAGE_READ_FAILED);
        }
    }

    public record ValidatedImage(byte[] bytes, String mimeType) {}
}

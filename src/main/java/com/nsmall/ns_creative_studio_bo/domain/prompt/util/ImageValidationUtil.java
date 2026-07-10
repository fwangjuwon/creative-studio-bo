package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 참조 이미지(MultipartFile) 검증 유틸 — 상품/인물 프롬프트 공용
 *
 * <pre>
 * 정책:
 *   - 허용 MIME: image/jpeg, image/png, image/webp
 *   - 최대 크기: 15MB
 *   - null/empty 입력은 "참조 이미지 없음"으로 간주하여 null 반환 (예외 아님)

 *   - 상태(필드)와 외부 의존성(주입 대상)이 없는 순수 함수형 검증 로직
 *
 *   
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
        validateMetadata(file);
        return readValidated(file);
    }

    /**
     * 참조 이미지 목록 검증.
     * empty 파일은 개수 제한 계산 전에 먼저 제외하고, 모든 파일의 크기/MIME 타입(메타데이터)을
     * 먼저 검사한 뒤에야 바이트를 읽는다 — 뒤쪽 파일이 검증에 실패해 요청 전체가 거부될 경우
     * 앞쪽 파일들을 불필요하게 메모리에 읽어들이는 낭비를 막는다.
     *
     * @param files 검증할 MultipartFile 목록 (null/empty 허용)
     * @return null/empty 입력 시 빈 리스트, 그 외에는 검증 통과한 이미지 목록 (empty 파일은 제외)
     * @throws BizException 개수 초과(IG015) / 개별 파일 검증 실패 (validate()와 동일)
     */
    public static List<ValidatedImage> validateList(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> nonEmptyFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (nonEmptyFiles.size() > PromptConstants.MAX_REFERENCE_IMAGES) {
            throw new BizException(ImageGenerationErrorCode.TOO_MANY_REFERENCE_IMAGES);
        }
        nonEmptyFiles.forEach(ImageValidationUtil::validateMetadata);
        return nonEmptyFiles.stream().map(ImageValidationUtil::readValidated).toList();
    }

    /** 크기/MIME 타입 검사 — 바이트를 읽지 않는 저비용 검증 */
    private static void validateMetadata(MultipartFile file) {
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
    }

    /** 메타데이터 검증 통과 후 바이트를 읽어 ValidatedImage 로 변환 */
    private static ValidatedImage readValidated(MultipartFile file) {
        try {
            return new ValidatedImage(file.getBytes(), file.getContentType());
        } catch (IOException e) {
            log.error("참조 이미지 읽기 실패", e);
            throw new BizException(ImageGenerationErrorCode.IMAGE_READ_FAILED);
        }
    }

    /** base64 는 Gemini 요청 페이로드용으로 생성 시점에 1회만 인코딩해 재사용한다 (병렬 N회 생성 시 중복 인코딩 방지) */
    public record ValidatedImage(byte[] bytes, String mimeType, String base64) {
        public ValidatedImage(byte[] bytes, String mimeType) {
            this(bytes, mimeType, Base64.getEncoder().encodeToString(bytes));
        }
    }
}

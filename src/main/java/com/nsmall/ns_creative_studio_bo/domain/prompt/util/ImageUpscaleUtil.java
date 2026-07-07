package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 생성된 이미지를 별도 AI 호출 없이 Java 내장 Graphics2D로 업스케일하는 유틸리티.
 *
 * <pre>
 * Bicubic 보간 — 외부 업스케일 서버/API 의존성 없이 목표 해상도로 확대한다.
 * 미디어월 등 초광각/초장축 비율은 태그에 저장된 정확한 픽셀 크기로 확대하고,
 * 일반 비율은 scaleFactor 배율(기본 2x)로 확대한다.
 * BufferedImage는 힙을 많이 소비하므로 Semaphore로 동시 처리 수를 제한한다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-06-30
 */
public class ImageUpscaleUtil {

    // 동시 업스케일 최대 허용 수 — BufferedImage 힙 점유 방지 (2x 업스케일 기준 이미지당 ~16MB)
    private static final int MAX_CONCURRENT_UPSCALE = 20;
    private static final Semaphore UPSCALE_SEMAPHORE = new Semaphore(MAX_CONCURRENT_UPSCALE);

    private ImageUpscaleUtil() {}

    /** 목표 픽셀 크기를 지정하여 업스케일 (초광각/초장축 비율에서 정확한 목표 해상도가 있을 때 사용) */
    public static byte[] upscale(byte[] imageBytes, String mimeType, int targetWidth, int targetHeight) {
        try {
            if (!UPSCALE_SEMAPHORE.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
            }
            return draw(source, mimeType, targetWidth, targetHeight);
        } catch (IOException e) {
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        } finally {
            UPSCALE_SEMAPHORE.release();
        }
    }

    /** 배율을 지정하여 업스케일 (일반 비율에서 원본 크기의 N배로 확대할 때 사용) */
    public static byte[] upscale(byte[] imageBytes, String mimeType, double scaleFactor) {
        if (scaleFactor <= 0) {
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        }
        try {
            if (!UPSCALE_SEMAPHORE.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
            }
            int targetWidth = (int) (source.getWidth() * scaleFactor);
            int targetHeight = (int) (source.getHeight() * scaleFactor);
            return draw(source, mimeType, targetWidth, targetHeight);
        } catch (IOException e) {
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        } finally {
            UPSCALE_SEMAPHORE.release();
        }
    }

    private static byte[] draw(BufferedImage source, String mimeType, int targetWidth, int targetHeight)
            throws IOException {
        // PNG만 알파 채널 보존 (ARGB) — 그 외는 모두 JPEG로 출력 (webp 포함, ImageIO 기본 지원 없음)
        boolean isPng = mimeType != null && mimeType.equalsIgnoreCase("image/png");
        // PNG는 알파 채널 보존을 위해 ARGB 타입 사용 — RGB 타입 사용 시 투명 픽셀이 검정으로 합성됨
        int imageType = isPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        String format = isPng ? "png" : "jpeg";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(target, format, baos)) {
            throw new BizException(ImageGenerationErrorCode.IMAGE_UPSCALE_FAILED);
        }
        return baos.toByteArray();
    }
}

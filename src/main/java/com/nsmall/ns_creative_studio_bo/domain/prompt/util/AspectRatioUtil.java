package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

/**
 * 이미지 비율 판별 유틸리티
 *
 * @author gaeddo
 * @since 2026-06-30
 */
public class AspectRatioUtil {

    private AspectRatioUtil() {}

    /**
     * 가로:세로 비율이 4:1 이상이면 true (미디어월 등 초광각 비율 감지용).
     */
    public static boolean isWideRatio(String aspectRatio) {
        return exceedsRatio(aspectRatio, 4.0, false);
    }

    /**
     * 세로:가로 비율이 2:1 이상이면 true (세로로 긴 초장축 비율 감지용).
     */
    public static boolean isTallRatio(String aspectRatio) {
        return exceedsRatio(aspectRatio, 2.0, true);
    }

    private static boolean exceedsRatio(String aspectRatio, double threshold, boolean inverted) {
        if (aspectRatio == null) return false;
        try {
            String[] parts = aspectRatio.split(":");
            if (parts.length != 2) return false;
            double width = Double.parseDouble(parts[0]);
            double height = Double.parseDouble(parts[1]);
            if (width <= 0 || height <= 0) return false;
            double ratio = inverted ? height / width : width / height;
            return ratio >= threshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 초광각/초장축 비율 태그(예: "9216:1728")의 숫자를 목표 픽셀 크기로 파싱한다.
     * 미디어월 등은 태그 값 자체가 실제 목표 해상도(px)로 등록되어 있다 (db/data.sql 참조).
     * 일반 비율(1:1, 16:9 등)에는 사용하지 않는다 — isWideRatio/isTallRatio가 true인 경우에만 호출할 것.
     *
     * @return {targetWidth, targetHeight}, 파싱 실패 시 null
     */
    private static final int MAX_PIXEL = 30_000;

    public static int[] parseTargetDimensions(String aspectRatio) {
        if (aspectRatio == null) return null;
        try {
            String[] parts = aspectRatio.split(":");
            if (parts.length != 2) return null;
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            if (width <= 0 || height <= 0 || width > MAX_PIXEL || height > MAX_PIXEL) return null;
            return new int[]{width, height};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

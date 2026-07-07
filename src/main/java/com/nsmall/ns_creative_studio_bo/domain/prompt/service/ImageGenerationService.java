package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import java.util.List;

/**
 * 이미지 생성 서비스 인터페이스
 *
 * <pre>
 * 영문 프롬프트 텍스트를 받아 이미지를 생성하고 서버에 저장 후 접근 URL을 반환한다.
 * 인물 사진(NanoBanana) 및 추후 상품 사진 등 다양한 구현체에서 재사용 가능.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-04
 */
public interface ImageGenerationService {

    /**
     * 영문 프롬프트와 참조 이미지(byte[])로 이미지를 생성하고 서버에 저장 후 접근 URL을 반환한다.
     * 참조 이미지를 스타일/구도 가이드로 활용하여 새 이미지를 생성한다 (인물·상품 참조이미지 기반 생성용).
     *
     * @param prompt           영문 이미지 생성 프롬프트
     * @param imageBytes       참조 이미지 바이너리
     * @param mimeType         참조 이미지 MIME 타입 (예: image/jpeg)
     * @param imageAspectRatio 사용자가 선택한 이미지 비율 태그값 (nullable) — 초광각/초장축 비율이면
     *                         모델은 기본 해상도로 생성 후 Java에서 목표 해상도로 업스케일한다
     * @return 생성된 이미지 접근 URL (예: /images/{uuid}.jpg)
     */
    String generateImageMime(String prompt, byte[] imageBytes, String mimeType, String imageAspectRatio);

    /**
     * 기존 완성 이미지에 텍스트를 합성하여 count개 병렬 생성 후 URL 목록을 반환한다.
     * TEXT_OVERLAY_SYSTEM_PROMPT를 주입하여 원본 이미지 보존 + 텍스트 추가만 수행한다.
     *
     * @param prompt     텍스트 합성 지시 프롬프트
     * @param imageBytes 원본 이미지 바이너리
     * @param mimeType   원본 이미지 MIME 타입 (예: image/jpeg)
     * @param count      생성할 이미지 수
     * @return 생성된 이미지 접근 URL 목록
     */
    List<String> generateTextOverlayImages(String prompt, byte[] imageBytes, String mimeType, int count);

    /**
     * 영문 프롬프트로 이미지를 count개 병렬 생성하고 접근 URL 목록을 반환한다.
     *
     * @param prompt           이미지 생성에 사용할 영문 프롬프트 텍스트
     * @param count            생성할 이미지 수
     * @param imageAspectRatio 사용자가 선택한 이미지 비율 태그값 (nullable) — 초광각/초장축 비율이면
     *                         모델은 기본 해상도로 생성 후 Java에서 목표 해상도로 업스케일한다
     * @return 생성된 이미지 접근 URL 목록
     */
    List<String> generateImages(String prompt, int count, String imageAspectRatio);

    /**
     * 인물 이미지 + 상품 이미지 두 장을 합성하여 이미지를 count개 병렬 생성한다.
     *
     * @param userMessage       합성 지시 사항 (포즈, 추가 지시사항 등)
     * @param personImageBytes  인물 참조 이미지 바이너리
     * @param personMimeType    인물 이미지 MIME 타입
     * @param productImageBytes 상품 참조 이미지 바이너리
     * @param productMimeType   상품 이미지 MIME 타입
     * @param count             생성할 이미지 수
     * @param imageAspectRatio  사용자가 선택한 이미지 비율 태그값 (nullable) — 초광각/초장축 비율이면
     *                          모델은 기본 해상도로 생성 후 Java에서 목표 해상도로 업스케일한다
     * @return 생성된 이미지 접근 URL 목록
     */
    List<String> generateFusionImages(String userMessage,
                                      byte[] personImageBytes, String personMimeType,
                                      byte[] productImageBytes, String productMimeType,
                                      int count, String imageAspectRatio);

    /**
     * 저장된 이미지 파일을 삭제한다. 병렬 생성 중 일부 실패 시 이미 저장된 파일 정리에 사용.
     *
     * @param relativeUrl 삭제할 이미지의 상대 경로 (예: /images/{uuid}.jpg)
     */
    void deleteImageFile(String relativeUrl);
}

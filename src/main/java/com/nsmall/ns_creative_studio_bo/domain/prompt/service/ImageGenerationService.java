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
     *
     * @param prompt     영문 이미지 생성 프롬프트
     * @param imageBytes 참조 이미지 바이너리
     * @param mimeType   참조 이미지 MIME 타입 (예: image/jpeg)
     * @return 생성된 이미지 접근 URL (예: /images/{uuid}.jpg)
     */
    String generateImageMime(String prompt, byte[] imageBytes, String mimeType);

    /**
     * 영문 프롬프트로 이미지를 count개 병렬 생성하고 접근 URL 목록을 반환한다.
     *
     * @param prompt 이미지 생성에 사용할 영문 프롬프트 텍스트
     * @param count  생성할 이미지 수
     * @return 생성된 이미지 접근 URL 목록
     */
    List<String> generateImages(String prompt, int count);

    /**
     * 인물 이미지 + 상품 이미지 두 장을 합성하여 이미지를 count개 병렬 생성한다.
     *
     * @param userMessage       합성 지시 사항 (포즈, 추가 지시사항 등)
     * @param personImageBytes  인물 참조 이미지 바이너리
     * @param personMimeType    인물 이미지 MIME 타입
     * @param productImageBytes 상품 참조 이미지 바이너리
     * @param productMimeType   상품 이미지 MIME 타입
     * @param count             생성할 이미지 수
     * @return 생성된 이미지 접근 URL 목록
     */
    List<String> generateFusionImages(String userMessage,
                                      byte[] personImageBytes, String personMimeType,
                                      byte[] productImageBytes, String productMimeType,
                                      int count);
}

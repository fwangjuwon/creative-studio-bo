package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 합성 이미지 생성 서비스 인터페이스
 *
 * <pre>
 * 상품 이미지 + 인물 이미지 두 장을 받아 NanaBanana(Gemini 이미지 모델)로
 * 자연스러운 합성 이미지를 생성하고 접근 가능한 절대 URL 목록을 반환한다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-19
 */
public interface FusionImageService {

    /**
     * 상품 이미지와 인물 이미지를 합성하여 이미지 URL 목록을 반환한다.
     *
     * @param productImage     상품 참조 이미지 (필수)
     * @param personImage      인물 참조 이미지 (필수)
     * @param pose             포즈/행동 지시사항 (nullable)
     * @param instruction      추가 지시사항 (nullable)
     * @param imageAspectRatio 이미지 비율 (nullable, 예: "1:1", "9:16")
     * @return 생성된 합성 이미지 절대 URL 목록
     */
    List<String> generateFusionImages(MultipartFile productImage, MultipartFile personImage,
                                      String pose, String instruction, String imageAspectRatio);
}

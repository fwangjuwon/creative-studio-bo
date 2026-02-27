package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import java.util.List;

/**
 * Gemini API 호출 서비스 인터페이스
 *
 * <pre>
 * 카테고리명과 태그명 목록을 받아 Gemini API를 호출하고
 * 영문 이미지 생성 프롬프트 텍스트를 반환한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface GeminiService {

    /**
     * 카테고리와 태그 정보를 기반으로 영문 이미지 생성 프롬프트를 생성한다.
     *
     * @param categoryName 선택한 상품 카테고리명
     * @param tagNames     선택한 태그명 목록
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String generatePrompt(String categoryName, List<String> tagNames);
}

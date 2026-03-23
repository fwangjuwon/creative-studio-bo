package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import java.util.List;
import java.util.Map;

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
     * 상품 카테고리와 태그 정보를 기반으로 영문 이미지 생성 프롬프트를 생성한다.
     *
     * @param goodsCategoryName 선택한 상품 카테고리명 (LCS→SCS 전체 경로)
     * @param tagNames          선택한 태그명 목록 (이미지 사이즈 태그 제외)
     * @param imageAspectRatio  이미지 비율 (nullable, 예: "1:1", "4:3", "16:9", "9:16")
     * @param purpose           생성 목적 (nullable)
     * @param additionalNotes   추가 지시 사항 (nullable)
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String generateGoodsPrompt(String goodsCategoryName, List<String> tagNames,
                               String imageAspectRatio, String purpose, String additionalNotes);

    /**
     * 5단계 구조적 선택 기반 인간 이미지 전용 영문 프롬프트를 생성한다.
     *
     * @param stepOptionValues stepNumber(1~5) → 선택된 prompt_value 목록
     * @param purpose          생성 목적 (nullable)
     * @param additionalNotes  추가 지시 사항 (nullable)
     * @param promptStyle      프롬프트 스타일 — {@code "NANO_BANANA"} (문장형) 또는
     *                         {@code "MID_JOURNEY"} (키워드 나열형)
     * @param imageAspectRatio 이미지 비율 (nullable, 예: "1:1", "4:3", "16:9", "9:16")
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String generateHumanPrompt(Map<Integer, List<String>> stepOptionValues,
                               String purpose, String additionalNotes, String promptStyle,
                               String imageAspectRatio);
}

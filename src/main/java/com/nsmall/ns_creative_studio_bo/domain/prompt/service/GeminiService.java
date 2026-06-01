package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;

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
     * @param promptStyle       프롬프트 스타일 ({@link PromptStyle#NANO_BANANA}: 문장형,
     *                          {@link PromptStyle#MID_JOURNEY}: 키워드 나열형)
     * @param textOverlay       이미지 내 삽입할 텍스트 오버레이 정보 (nullable, 없으면 미적용)
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String generateGoodsPrompt(String goodsCategoryName, List<String> tagNames,
                               String imageAspectRatio, String purpose, String additionalNotes,
                               PromptStyle promptStyle,
                               TextOverlayInput textOverlay);

    /**
     * 인물별 단계 선택 기반 영문 프롬프트를 생성한다.
     *
     * <pre>
     * - 1~5명 개별 모드: personsStepValues에 인물 수만큼 Map이 담김
     * - 6명 이상 단체 모드: personsStepValues에 공통 옵션 Map 1개 + isGroup=true
     *
     * 데이터 구조: stepName → (attrName → promptValues)
     * Gemini 프롬프트에서 각 속성을 개별 라인으로 출력하여
     * 복수 속성(연령대 + 성별)을 여러 인물로 오해하는 버그를 방지한다.
     * </pre>
     *
     * @param personsStepValues 인물별 stepName → (attrName → promptValues) (인물 수만큼)
     * @param commonStepValues  공통 단계 stepName → (attrName → promptValues) (조명/촬영 등)
     * @param isGroup           6명 이상 단체 모드 여부
     * @param relationship      인물 간 관계 (nullable)
     * @param additionalNotes   추가 지시 사항 (nullable)
     * @param promptStyle       프롬프트 스타일
     * @param imageAspectRatio  이미지 비율 (nullable)
     * @param textOverlay       이미지 내 삽입할 텍스트 오버레이 정보 (nullable, 없으면 미적용)
     * @return Gemini가 생성한 영문 프롬프트 텍스트
     */
    String generateHumanPrompt(List<Map<String, Map<String, List<String>>>> personsStepValues,
                               Map<String, Map<String, List<String>>> commonStepValues,
                               boolean isGroup,
                               String relationship, String additionalNotes, PromptStyle promptStyle,
                               String imageAspectRatio,
                               TextOverlayInput textOverlay);
}

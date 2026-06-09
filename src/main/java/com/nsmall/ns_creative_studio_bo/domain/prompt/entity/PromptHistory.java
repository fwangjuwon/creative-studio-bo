package com.nsmall.ns_creative_studio_bo.domain.prompt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PromptHistory 엔티티 — {@code prompt_history} 테이블 매핑
 *
 * <pre>
 * 사용자가 생성한 프롬프트 이력 (상품/인물 공통).
 * user_id     : 논리 FK → User (DB 제약 없음)
 * category_id : 논리 FK → Category SCS (상품 프롬프트만, 인물은 null)
 * prompt_type : 'PRODUCT' | 'PERSON'
 * prompt_style: 'NANO_BANANA' | 'MID_JOURNEY'
 * purpose, additional_notes: 생성 목적 (선택, 상품·인물 공통)
 * init_regi_dttm 기준으로 일일 사용량을 집계한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistory {

    private Long id;
    private Long userId;
    private Long categoryId;         // 상품 프롬프트만 값 있음, 인물 프롬프트는 null
    private String promptType;       // 'PRODUCT' | 'PERSON'
    private String promptStyle;      // 'NANO_BANANA' | 'MID_JOURNEY'
    private String promptText;

    private String imageUrl;          // NanoBanana 이미지 URL (없으면 null)
    private String imageAspectRatio; // 선택한 이미지 종횡비 (예: "16:9", "1:1", nullable)
    private String purpose;          // 생성 목적 (선택, 상품·인물 공통)
    private String additionalNotes;  // 추가 지시 사항 (선택, 상품·인물 공통)

    // 텍스트 오버레이
    private String textContent;        // 삽입할 텍스트 내용 (없으면 null) — AI가 이미지 분위기에 맞게 자동 처리

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

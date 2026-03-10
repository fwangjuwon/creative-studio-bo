package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상품(Goods) 프롬프트 생성 요청 DTO
 *
 * <pre>
 * goodsCategoryId: 최종 선택한 DCS 카테고리 ID (상품군)
 * tagIds         : 선택한 태그 ID 목록 (빈 리스트 허용)
 * promptStyle    : 프롬프트 스타일 (선택, 기본값: NANO_BANANA)
 *                  NANO_BANANA — 문장형 (NanoBanana/Gemini 최적화)
 *                  MID_JOURNEY — 키워드 나열형 (MidJourney 최적화)
 *
 * 요청 사용자 ID는 JWT SecurityContext에서 추출 — 클라이언트가 전달하지 않음
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsPromptCreateReqDto {

    @NotNull(message = "상품 카테고리 ID는 필수입니다.")
    private Long goodsCategoryId;

    @Builder.Default
    private List<Long> tagIds = List.of();

    /** 프롬프트 스타일 (선택, 미입력 시 NANO_BANANA 기본값 적용) */
    private String promptStyle;
}

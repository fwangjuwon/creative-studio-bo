package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상품(Goods) 프롬프트 생성 요청 DTO
 *
 * <pre>
 * goodsCategoryId: 최종 선택한 SCS 카테고리 ID (상품군)
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
    private PromptStyle promptStyle;

    /** 이미지 비율 (선택, 예: "1:1", "4:3", "16:9", "9:16") — 이미지 사이즈 태그와 연동 */
    private String imageAspectRatio;

    private String purpose;          // 생성 목적 (선택)
    private String additionalNotes;  // 추가 지시 사항 (선택)

    // 텍스트 오버레이 — 입력 시 AI가 이미지 분위기에 맞게 자동 처리
    @Size(max = 500, message = "텍스트 문구는 500자 이하여야 합니다.")
    private String textContent;      // 삽입할 메인 텍스트 (없으면 오버레이 비활성화)

    /** 단서 문구 텍스트 — 메인 텍스트 아래 작은 글씨로 배치 (없으면 단서 문구 없음) */
    @Size(max = 300, message = "단서 문구는 300자 이하여야 합니다.")
    private String disclaimerText;
}

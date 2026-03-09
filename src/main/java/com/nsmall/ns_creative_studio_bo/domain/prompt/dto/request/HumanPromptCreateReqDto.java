package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 인간 이미지 프롬프트 생성 요청 DTO
 *
 * <pre>
 * POST /api/human-prompts 요청 바디.
 * selections: key = attrId, value = 선택한 optionId 목록 (단일선택도 List로 통일)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptCreateReqDto {

    /**
     * 선택한 항목-선택지 맵.
     * key: attrId, value: 선택된 optionId 목록 (1개 이상)
     */
    @NotNull(message = "selections는 필수입니다.")
    private Map<Long, List<Long>> selections;

    /** 생성 목적 (선택) */
    private String purpose;

    /** 추가 지시 사항 (선택) */
    private String additionalNotes;

    /**
     * 프롬프트 스타일 (선택, 기본값: NANO_BANANA).
     * <ul>
     *   <li>{@code NANO_BANANA} — 문장형 (NanaBanana/Gemini 최적화)</li>
     *   <li>{@code MID_JOURNEY} — 키워드 나열형 (MidJourney 최적화)</li>
     * </ul>
     */
    private String promptStyle;
}

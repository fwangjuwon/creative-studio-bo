package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 인물 이미지 프롬프트 생성 요청 DTO
 *
 * <pre>
 * POST /api/human-prompts 요청 바디.
 *
 * 인원수에 따라 두 가지 모드로 동작한다:
 *   - 1~5명 (개별 모드): persons 배열에 인물별 selections를 각각 담아 전달
 *   - 6명 이상 (단체 모드): isGroup=true + selections에 공통 옵션 1세트 전달
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
     * 인물별 선택 옵션 목록 (1~5명 개별 모드).
     * 각 원소가 인물 1명의 옵션 선택을 나타낸다.
     * isGroup=true인 경우 null.
     */
    private List<PersonSelectionDto> persons;

    /**
     * 6명 이상 단체 모드 여부.
     * true이면 persons 대신 selections(공통 옵션)를 사용한다.
     */
    private Boolean isGroup;

    /**
     * 공통 옵션 (6명 이상 단체 모드 전용).
     * key: attrId, value: 선택된 optionId 목록
     * isGroup=false인 경우 null.
     */
    private Map<Long, List<Long>> selections;

    /**
     * 공통 단계 선택 (is_common=true인 step의 옵션).
     * 1~5명 개별 모드 / 6명 이상 단체 모드 모두 공통으로 사용.
     * key: attrId, value: 선택된 optionId 목록
     */
    private Map<Long, List<Long>> commonSelections;

    /**
     * 인물 간 관계 (2명 이상 선택 시 선택 입력).
     * 예: "커플", "친구", "가족", "직장 동료"
     */
    @Size(max = 100, message = "인물 간 관계는 100자 이하로 입력해 주세요.")
    private String relationship;

    /** 추가 지시 사항 (선택) */
    private String additionalNotes;

    /**
     * 프롬프트 스타일 (선택, 기본값: NANO_BANANA).
     * <ul>
     *   <li>{@code NANO_BANANA} — 문장형 (NanoBanana/Gemini 최적화)</li>
     *   <li>{@code MID_JOURNEY} — 키워드 나열형 (MidJourney 최적화)</li>
     * </ul>
     */
    private PromptStyle promptStyle;

    /** 이미지 비율 (선택, 예: "1:1", "4:3", "16:9", "9:16") */
    private String imageAspectRatio;

    // 텍스트 오버레이 — 입력 시 AI가 이미지 분위기에 맞게 자동 처리
    /** 삽입할 메인 텍스트 (없으면 오버레이 비활성화) */
    @Size(max = 500, message = "텍스트 문구는 500자 이하여야 합니다.")
    private String textContent;

    /** 단서 문구 텍스트 — 메인 텍스트 아래 작은 글씨로 배치 (없으면 단서 문구 없음) */
    @Size(max = 300, message = "단서 문구는 300자 이하여야 합니다.")
    private String disclaimerText;
}

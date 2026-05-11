package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 인물 1인분 옵션 선택 DTO
 *
 * <pre>
 * HumanPromptCreateReqDto.persons 배열의 각 원소.
 * 1~5명 개별 선택 모드에서 인물 1명의 선택 정보를 담는다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-06
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonSelectionDto {

    /**
     * 선택한 항목-선택지 맵.
     * key: attrId, value: 선택된 optionId 목록
     */
    private Map<Long, List<Long>> selections;
}

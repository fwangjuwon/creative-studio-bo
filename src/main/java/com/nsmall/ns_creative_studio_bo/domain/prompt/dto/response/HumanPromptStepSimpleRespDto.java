package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 단계 단순 응답 DTO
 *
 * <pre>
 * Step CUD API 응답에 사용.
 * 기존 HumanPromptStepRespDto는 Step → Attr → Option 중첩 구조이므로
 * CUD 응답용으로 별도 단순 DTO를 사용한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStepSimpleRespDto {

    private Long id;
    private int stepNumber;
    private String stepCode;
    private String name;

    public static HumanPromptStepSimpleRespDto from(HumanPromptStep step) {
        return HumanPromptStepSimpleRespDto.builder()
                .id(step.getId())
                .stepNumber(step.getStepNumber())
                .stepCode(step.getStepCode())
                .name(step.getName())
                .build();
    }
}

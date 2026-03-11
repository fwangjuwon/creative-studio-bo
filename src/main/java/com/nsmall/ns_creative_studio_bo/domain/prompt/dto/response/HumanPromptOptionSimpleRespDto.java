package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 선택지 단순 응답 DTO
 *
 * <pre>
 * Option CUD API 응답에 사용.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptOptionSimpleRespDto {

    private Long id;
    private Long attrId;
    private String name;
    private String promptValue;
    private int sortOrder;

    public static HumanPromptOptionSimpleRespDto from(HumanPromptOption option) {
        return HumanPromptOptionSimpleRespDto.builder()
                .id(option.getId())
                .attrId(option.getAttrId())
                .name(option.getName())
                .promptValue(option.getPromptValue())
                .sortOrder(option.getSortOrder())
                .build();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 인간 이미지 프롬프트 단계 응답 DTO — Step → Attr → Option 중첩 구조
 *
 * <pre>
 * GET /api/human-prompts/steps 응답에 사용.
 * MyBatis ResultMap + collection으로 중첩 매핑된다.
 * Lombok @Builder 미사용 — MyBatis가 기본 생성자 + setter로 객체를 채운다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Getter
@Setter
@NoArgsConstructor
public class HumanPromptStepRespDto {

    private Long id;
    private int stepNumber;
    private String stepCode;
    private String name;
    private List<AttrDto> attrs;

    /**
     * 세부 항목 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class AttrDto {
        private Long id;
        private String name;
        private boolean multiSelect;
        private boolean required;
        private int sortOrder;
        private List<OptionDto> options;
    }

    /**
     * 선택지 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OptionDto {
        private Long id;
        private String name;
        private String promptValue;
        private int sortOrder;
    }
}

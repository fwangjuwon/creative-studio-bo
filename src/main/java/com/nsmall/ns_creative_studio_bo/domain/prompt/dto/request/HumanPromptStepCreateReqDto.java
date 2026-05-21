package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인간 이미지 프롬프트 단계 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanPromptStepCreateReqDto {

    /** 단계 번호 (선택 사항) — null이면 서비스에서 현재 최대값 + 1로 자동 세팅 */
    private Integer stepNumber;

    /** 단계 코드 (선택 사항) — null이면 서비스에서 stepNumber 기반으로 자동 생성 (1→A, 2→B ...) */
    @Size(max = 2, message = "단계 코드는 2자 이하여야 합니다.")
    private String stepCode;

    @NotBlank(message = "단계명은 필수입니다.")
    @Size(max = 100, message = "단계명은 100자 이하여야 합니다.")
    private String name;

    /** true: 전원 공통 적용 단계 / false: 인물별 개별 선택 단계 (기본값: false) */
    @JsonProperty("isCommon")
    private boolean isCommon;
}

package com.nsmall.ns_creative_studio_bo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * NsistEmpMapping 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsistEmpMappingCreateReqDto {

    @NotBlank(message = "사원번호는 필수입니다.")
    private String empCd;

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
}

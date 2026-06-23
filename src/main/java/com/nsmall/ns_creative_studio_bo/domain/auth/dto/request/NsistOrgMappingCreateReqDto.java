package com.nsmall.ns_creative_studio_bo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * NsistOrgMapping 생성 요청 DTO
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsistOrgMappingCreateReqDto {

    @NotBlank(message = "조직코드는 필수입니다.")
    @Size(max = 20, message = "조직코드는 20자 이하여야 합니다.")
    private String orgCd;

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
}

package com.nsmall.ns_creative_studio_bo.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 본인 비밀번호 변경 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-06-08
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordUpdateReqDto {

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    private String newPassword;
}

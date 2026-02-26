package com.nsmall.ns_creative_studio_bo.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User 생성 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateReqDto {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "권한은 필수입니다.")
    @Pattern(regexp = "^(ADMIN|MANAGER|USER)$", message = "권한은 ADMIN, MANAGER, USER 중 하나여야 합니다.")
    private String role;

    private String teamName;

    @NotNull(message = "일일 생성 한도는 필수입니다.")
    @Min(value = 0, message = "일일 생성 한도는 0 이상이어야 합니다.")
    private Integer dailyLimit;
}

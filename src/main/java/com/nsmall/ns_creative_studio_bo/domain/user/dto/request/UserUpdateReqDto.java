package com.nsmall.ns_creative_studio_bo.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User 수정 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateReqDto {

    private String password;

    private String name;

    @Pattern(regexp = "^(ADMIN|MANAGER|USER)$", message = "권한은 ADMIN, MANAGER, USER 중 하나여야 합니다.")
    private String role;

    private String teamName;

    @Min(value = 0, message = "일일 생성 한도는 0 이상이어야 합니다.")
    private Integer dailyLimit;
}

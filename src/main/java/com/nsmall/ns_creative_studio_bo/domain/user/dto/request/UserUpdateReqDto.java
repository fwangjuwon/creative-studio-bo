package com.nsmall.ns_creative_studio_bo.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @Pattern(regexp = "^(ADMIN|MANAGER|USER)$", message = "권한은 ADMIN, MANAGER, USER 중 하나여야 합니다.")
    private String role;

    @Size(max = 100, message = "팀명은 100자 이하여야 합니다.")
    private String teamName;

    @Min(value = 0, message = "일일 생성 한도는 0 이상이어야 합니다.")
    private Integer dailyLimit;

    @Pattern(regexp = "^(Y|N)$", message = "계정 활성화 여부는 Y 또는 N 이어야 합니다.")
    private String useYn;
}

package com.nsmall.ns_creative_studio_bo.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO — JWT Access Token 포함
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRespDto {

    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public static LoginRespDto of(String accessToken, long expiresIn) {
        return LoginRespDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}

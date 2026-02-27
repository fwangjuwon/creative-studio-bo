package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;

/**
 * Auth 서비스 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
public interface AuthService {

    /**
     * 로그인 — loginId/password 검증 후 JWT Access Token 발급
     *
     * @param dto 로그인 요청 (loginId, password)
     * @return JWT Access Token 및 만료 정보
     */
    LoginRespDto login(LoginReqDto dto);
}

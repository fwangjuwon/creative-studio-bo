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

    /**
     * nsist SSO 로그인 — nsist 서비스에서 전달된 empCd로 사용자를 식별하여 JWT Access Token 발급.
     *
     * <pre>
     * empCd → loginId 매핑은 application-{profile}.yml의 nsist.emp-cd-map으로 관리
     * (추후 DB emp_cd 컬럼 기반으로 교체 예정).
     * </pre>
     *
     * @param empCd nsist에서 전달된 사원 코드
     * @param orgCd nsist에서 전달된 조직 코드 (현재 미사용, 추후 확장 대비)
     * @return JWT Access Token 및 만료 정보
     */
    LoginRespDto nsistLogin(String empCd, String orgCd);
}

package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import jakarta.servlet.http.HttpServletRequest;

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
     * 보안 정책: Referer 헤더가 https://nsist.nsmall.com 으로 시작하는 요청만 허용.
     * empCd → loginId 매핑은 현재 하드코딩으로 관리 (추후 DB emp_cd 컬럼 기반으로 교체 예정).
     * </pre>
     *
     * @param empCd   nsist에서 전달된 사원 코드
     * @param orgCd   nsist에서 전달된 조직 코드 (현재 미사용, 추후 확장 대비)
     * @param request Referer 헤더 검증에 사용할 HTTP 요청 객체
     * @return JWT Access Token 및 만료 정보
     */
    LoginRespDto nsistLogin(String empCd, String orgCd, HttpServletRequest request);
}

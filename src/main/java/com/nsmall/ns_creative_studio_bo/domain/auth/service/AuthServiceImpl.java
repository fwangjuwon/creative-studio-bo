package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.config.JwtProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.security.JwtTokenProvider;
import com.nsmall.ns_creative_studio_bo.domain.auth.AuthErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Auth 서비스 구현체 — 로그인 및 JWT 토큰 발급
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String NSIST_REFERER_PREFIX = "https://nsist.nsmall.com";

    /**
     * nsist empCd → NCS loginId 매핑 (임시 하드코딩).
     * 추후 user 테이블의 emp_cd 컬럼 기반으로 교체 예정.
     */
    private static final Map<String, String> NSIST_EMP_CD_MAP = Map.of(
        "12496", "jjhoo8936"
    );

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public LoginRespDto login(LoginReqDto dto) {
        User user = userMapper.selectByLoginId(dto.getLoginId())
                .orElseThrow(() -> new BizException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치 — loginId={}", dto.getLoginId());
            throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(user.getLoginId(), user.getRole());
        log.info("로그인 성공 — loginId={}, role={}", user.getLoginId(), user.getRole());

        return LoginRespDto.of(token, jwtProperties.getExpiration());
    }

    @Override
    public LoginRespDto nsistLogin(String empCd, String orgCd, HttpServletRequest request) {
        validateNsistReferer(request);

        String loginId = NSIST_EMP_CD_MAP.get(empCd);
        if (!StringUtils.hasText(loginId)) {
            log.warn("nsist SSO 로그인 실패 — 매핑되지 않은 empCd={}", empCd);
            throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        User user = userMapper.selectByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("nsist SSO 로그인 실패 — 사용자 미존재 loginId={}", loginId);
                    return new BizException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        String token = jwtTokenProvider.generateToken(user.getLoginId(), user.getRole());
        log.info("nsist SSO 로그인 성공 — empCd={}, loginId={}, role={}", empCd, user.getLoginId(), user.getRole());

        return LoginRespDto.of(token, jwtProperties.getExpiration());
    }

    private void validateNsistReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (!StringUtils.hasText(referer) || !referer.startsWith(NSIST_REFERER_PREFIX)) {
            log.warn("nsist SSO 로그인 거부 — 허용되지 않는 Referer: {}", referer);
            throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}

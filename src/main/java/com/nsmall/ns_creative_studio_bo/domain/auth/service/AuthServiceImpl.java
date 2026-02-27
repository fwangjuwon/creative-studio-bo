package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.config.JwtProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.security.JwtTokenProvider;
import com.nsmall.ns_creative_studio_bo.domain.auth.AuthErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}

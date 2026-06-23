package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.config.JwtProperties;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.security.JwtTokenProvider;
import com.nsmall.ns_creative_studio_bo.domain.auth.AuthErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistEmpMapping;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistOrgMapping;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistEmpMappingMapper;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistOrgMappingMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.UserErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auth 서비스 구현체 — 로그인 및 JWT 토큰 발급
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final NsistEmpMappingMapper nsistEmpMappingMapper;
    private final NsistOrgMappingMapper nsistOrgMappingMapper;

    @Override
    public LoginRespDto login(LoginReqDto dto) {
        User user = userMapper.selectByLoginId(dto.getLoginId())
                .orElseThrow(() -> new BizException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치 — loginId={}", dto.getLoginId());
            throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!"Y".equals(user.getUseYn())) {
            log.warn("비활성화된 계정 로그인 시도 — loginId={}", dto.getLoginId());
            throw new BizException(UserErrorCode.USER_BANNED);
        }

        String token = jwtTokenProvider.generateToken(user.getLoginId(), user.getRole());
        log.info("로그인 성공 — loginId={}, role={}", user.getLoginId(), user.getRole());

        return LoginRespDto.of(token, jwtProperties.getExpiration());
    }

    @Override
    public LoginRespDto nsistLogin(String empCd, String orgCd) {
        // emp_cd 우선 조회 → 없으면 org_cd fallback
        Long userId = nsistEmpMappingMapper.selectByEmpCd(empCd)
                .map(NsistEmpMapping::getUserId)
                .orElseGet(() -> {
                    log.debug("nsist SSO — empCd 매핑 없음, orgCd fallback — empCd={}, orgCd={}", empCd, orgCd);
                    return nsistOrgMappingMapper.selectByOrgCd(orgCd)
                            .map(NsistOrgMapping::getUserId)
                            .orElseThrow(() -> {
                                log.warn("nsist SSO 로그인 실패 — empCd, orgCd 모두 매핑 없음 empCd={}, orgCd={}", empCd, orgCd);
                                return new BizException(AuthErrorCode.INVALID_CREDENTIALS);
                            });
                });

        User user = userMapper.selectById(userId)
                .orElseThrow(() -> {
                    log.warn("nsist SSO 로그인 실패 — 사용자 미존재 userId={}", userId);
                    return new BizException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        if (!"Y".equals(user.getUseYn())) {
            log.warn("비활성화된 계정 nsist SSO 로그인 시도 — empCd={}, orgCd={}, loginId={}", empCd, orgCd, user.getLoginId());
            throw new BizException(UserErrorCode.USER_BANNED);
        }

        String token = jwtTokenProvider.generateToken(user.getLoginId(), user.getRole());
        log.info("nsist SSO 로그인 성공 — empCd={}, orgCd={}, loginId={}, role={}", empCd, orgCd, user.getLoginId(), user.getRole());

        return LoginRespDto.of(token, jwtProperties.getExpiration());
    }
}

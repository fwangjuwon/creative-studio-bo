package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.security.JwtTokenProvider;
import com.nsmall.ns_creative_studio_bo.domain.auth.AuthErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthService 통합 테스트 — 로그인 및 JWT 토큰 발급 검증
 *
 * <pre>
 * UserService로 임시 계정을 생성한 뒤 로그인을 검증한다.
 * 모든 CUD 테스트는 @Transactional + @Rollback으로 DB 상태를 복원한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("local")
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ================================================================
    // 테스트 데이터 헬퍼
    // ================================================================

    private void 유저_생성(String loginId, String role) {
        userService.createUser(UserCreateReqDto.builder()
                .loginId(loginId)
                .password("TestPass1!")
                .name("테스트_" + loginId)
                .role(role)
                .teamName("테스트팀")
                .dailyLimit(10)
                .build());
    }

    private LoginReqDto 로그인_요청(String loginId, String password) {
        return LoginReqDto.builder()
                .loginId(loginId)
                .password(password)
                .build();
    }

    // ================================================================
    // 로그인 성공
    // ================================================================

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "ADMIN"})
    @Transactional
    @Rollback
    @DisplayName("[성공] 로그인 - 역할별 정상 케이스 (USER / MANAGER / ADMIN)")
    void 성공_로그인_역할별(String role) {
        String loginId = "auth_role_" + role.toLowerCase();
        유저_생성(loginId, role);

        LoginRespDto result = authService.login(로그인_요청(loginId, "TestPass1!"));

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 로그인 - 발급 토큰 유효성 검증 (validateToken / loginId 추출)")
    void 성공_로그인_토큰_유효성_검증() {
        유저_생성("auth_jwt_valid", "USER");

        LoginRespDto result = authService.login(로그인_요청("auth_jwt_valid", "TestPass1!"));
        String token = result.getAccessToken();

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getLoginId(token)).isEqualTo("auth_jwt_valid");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 로그인 - 만료된 토큰은 validateToken false 반환")
    void 성공_로그인_위조_토큰_검증실패() {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.fake.signature";

        assertThat(jwtTokenProvider.validateToken(fakeToken)).isFalse();
    }

    // ================================================================
    // 로그인 예외
    // ================================================================

    @Test
    @DisplayName("[예외] 로그인 - 존재하지 않는 loginId")
    void 예외_로그인_존재하지않는_loginId() {
        assertThatThrownBy(() -> authService.login(로그인_요청("no_such_user_xyz", "anypass")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 로그인 - 비밀번호 불일치")
    void 예외_로그인_비밀번호_불일치() {
        유저_생성("auth_wrong_pw", "USER");

        assertThatThrownBy(() -> authService.login(로그인_요청("auth_wrong_pw", "WrongPass99!")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
    }
}

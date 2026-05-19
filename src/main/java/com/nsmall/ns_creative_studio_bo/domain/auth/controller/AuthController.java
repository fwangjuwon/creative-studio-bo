package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.LoginReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.LoginRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth API 컨트롤러 — {@code /api/auth}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Auth", description = "인증 API")
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "loginId/password로 인증 후 JWT Access Token 발급. 토큰 유효 시간은 1시간."
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken 반환")
    @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    @PostMapping("/login")
    public CommonResponse<LoginRespDto> login(@RequestBody @Valid LoginReqDto dto) {
        return CommonResponse.success(authService.login(dto));
    }

    @Operation(
            summary = "nsist SSO 로그인",
            description = "nsist 서비스에서 전달된 empCd로 사용자를 식별하여 JWT Access Token 발급."
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken 반환")
    @ApiResponse(responseCode = "401", description = "매핑되지 않은 empCd 또는 존재하지 않는 사용자")
    @GetMapping("/nsist-login")
    public CommonResponse<LoginRespDto> nsistLogin(
            @RequestParam @NotBlank(message = "사원번호는 필수입니다.") @Size(max = 6, message = "사원번호는 6자 이하여야 합니다.") String empCd,
            @RequestParam @NotBlank(message = "조직코드는 필수입니다.") String orgCd) {
        return CommonResponse.success(authService.nsistLogin(empCd, orgCd));
    }
}

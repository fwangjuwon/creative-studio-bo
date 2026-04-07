package com.nsmall.ns_creative_studio_bo.domain.user.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserUsageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * User API 컨트롤러 — {@code /api/users}
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Tag(name = "User", description = "회원 관리 API")
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 생성", description = "ADMIN 전용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "회원 생성 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CommonResponse<UserRespDto> createUser(@RequestBody @Valid UserCreateReqDto dto) {
        return CommonResponse.success(userService.createUser(dto));
    }

    @Operation(summary = "회원 단건 조회", description = "ADMIN 전용 — 존재하지 않는 id면 200 + data: null 반환",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public CommonResponse<UserRespDto> getUser(@PathVariable @Positive Long id) {
        Optional<UserRespDto> result = userService.getUser(id);
        return CommonResponse.success(result.orElse(null));
    }

    @Operation(summary = "회원 목록 조회", description = "ADMIN 전용 — name / teamName / role 검색 조건 선택 가능",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public CommonResponse<List<UserRespDto>> getUsers(UserSearchReqDto searchDto) {
        return CommonResponse.success(userService.getUsers(searchDto));
    }

    @Operation(summary = "회원 수정", description = "ADMIN 전용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CommonResponse<UserRespDto> updateUser(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UserUpdateReqDto dto) {
        return CommonResponse.success(userService.updateUser(id, dto));
    }

    @Operation(summary = "회원 삭제", description = "ADMIN 전용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteUser(@PathVariable @Positive Long id) {
        userService.deleteUser(id);
        return CommonResponse.success();
    }

    @Operation(summary = "내 사용량 조회",
            description = "오늘 사용 횟수 및 남은 횟수 확인. USER / MANAGER / ADMIN 모든 인증 사용자 허용.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @GetMapping("/me/usage")
    public CommonResponse<UserUsageRespDto> getMyUsage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return CommonResponse.success(userService.getUserUsage(userDetails.getId()));
    }

    @Operation(summary = "특정 회원 사용량 조회", description = "ADMIN 전용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/usage")
    public CommonResponse<UserUsageRespDto> getUserUsage(@PathVariable @Positive Long id) {
        return CommonResponse.success(userService.getUserUsage(id));
    }

    @Operation(summary = "전체 회원 사용량 목록 조회",
            description = "ADMIN 전용 — 전체 회원의 금일 사용 횟수 및 남은 횟수를 단일 쿼리로 반환",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/usage")
    public CommonResponse<List<UserUsageRespDto>> getUserUsageList() {
        return CommonResponse.success(userService.getUserUsageList());
    }
}

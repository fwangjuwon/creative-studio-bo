package com.nsmall.ns_creative_studio_bo.domain.user.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 생성", description = "ADMIN 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "회원 생성 성공")
    @PostMapping
    public CommonResponse<UserRespDto> createUser(@RequestBody @Valid UserCreateReqDto dto) {
        return CommonResponse.success(userService.createUser(dto));
    }

    @Operation(summary = "회원 단건 조회", description = "존재하지 않는 id면 200 + data: null 반환")
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @GetMapping("/{id}")
    public CommonResponse<UserRespDto> getUser(@PathVariable Long id) {
        Optional<UserRespDto> result = userService.getUser(id);
        return CommonResponse.success(result.orElse(null));
    }

    @Operation(summary = "회원 목록 조회", description = "name / teamName / role 검색 조건 선택 가능 (미입력 시 전체 조회)")
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @GetMapping
    public CommonResponse<List<UserRespDto>> getUsers(UserSearchReqDto searchDto) {
        return CommonResponse.success(userService.getUsers(searchDto));
    }

    @Operation(summary = "회원 수정", description = "ADMIN 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public CommonResponse<UserRespDto> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateReqDto dto) {
        return CommonResponse.success(userService.updateUser(id, dto));
    }

    @Operation(summary = "회원 삭제", description = "ADMIN 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return CommonResponse.success();
    }
}

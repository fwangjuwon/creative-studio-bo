package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistEmpMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistEmpMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.NsistEmpMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NsistEmpMapping API 컨트롤러 — {@code /api/nsist-emp-mappings}
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Tag(name = "NsistEmpMapping", description = "nsist 사원번호↔NCS 계정 매핑 관리 API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/nsist-emp-mappings")
@RequiredArgsConstructor
public class NsistEmpMappingController {

    private final NsistEmpMappingService nsistEmpMappingService;

    @Operation(summary = "매핑 등록", description = "nsist 사원번호(empCd)와 NCS 사용자(userId)를 연결합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "매핑 등록 성공")
    @ApiResponse(responseCode = "400", description = "이미 등록된 사원번호 또는 존재하지 않는 사용자")
    @PostMapping
    public CommonResponse<NsistEmpMappingRespDto> createMapping(
            @RequestBody @Valid NsistEmpMappingCreateReqDto dto) {
        return CommonResponse.success(nsistEmpMappingService.createMapping(dto));
    }

    @Operation(summary = "매핑 목록 조회", description = "등록된 전체 사원번호↔NCS 계정 매핑 목록을 반환합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public CommonResponse<List<NsistEmpMappingRespDto>> getMappings() {
        return CommonResponse.success(nsistEmpMappingService.getMappings());
    }

    @Operation(summary = "매핑 삭제", description = "사원번호(empCd)로 매핑을 삭제합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 매핑")
    @DeleteMapping("/{empCd}")
    public CommonResponse<Void> deleteMapping(
            @PathVariable @NotBlank(message = "사원번호는 필수입니다.") @Size(max = 6, message = "사원번호는 6자 이하여야 합니다.") String empCd) {
        nsistEmpMappingService.deleteMapping(empCd);
        return CommonResponse.success();
    }
}

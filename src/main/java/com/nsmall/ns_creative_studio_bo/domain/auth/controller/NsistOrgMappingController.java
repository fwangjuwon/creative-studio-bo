package com.nsmall.ns_creative_studio_bo.domain.auth.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistOrgMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistOrgMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.service.NsistOrgMappingService;
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
 * NsistOrgMapping API 컨트롤러 — {@code /api/nsist-org-mappings}
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Tag(name = "NsistOrgMapping", description = "nsist 조직코드↔NCS 팀계정 매핑 관리 API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/nsist-org-mappings")
@RequiredArgsConstructor
public class NsistOrgMappingController {

    private final NsistOrgMappingService nsistOrgMappingService;

    @Operation(summary = "매핑 등록", description = "nsist 조직코드(orgCd)와 NCS 팀계정(userId)을 연결합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "매핑 등록 성공")
    @ApiResponse(responseCode = "400", description = "이미 등록된 조직코드 또는 존재하지 않는 사용자")
    @PostMapping
    public CommonResponse<NsistOrgMappingRespDto> createMapping(
            @RequestBody @Valid NsistOrgMappingCreateReqDto dto) {
        return CommonResponse.success(nsistOrgMappingService.createMapping(dto));
    }

    @Operation(summary = "매핑 목록 조회", description = "등록된 전체 조직코드↔NCS 팀계정 매핑 목록을 반환합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public CommonResponse<List<NsistOrgMappingRespDto>> getMappings() {
        return CommonResponse.success(nsistOrgMappingService.getMappings());
    }

    @Operation(summary = "매핑 삭제", description = "조직코드(orgCd)로 매핑을 삭제합니다. ADMIN 전용.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 매핑")
    @DeleteMapping("/{orgCd}")
    public CommonResponse<Void> deleteMapping(
            @PathVariable @NotBlank(message = "조직코드는 필수입니다.") @Size(max = 20, message = "조직코드는 20자 이하여야 합니다.") String orgCd) {
        nsistOrgMappingService.deleteMapping(orgCd);
        return CommonResponse.success();
    }
}

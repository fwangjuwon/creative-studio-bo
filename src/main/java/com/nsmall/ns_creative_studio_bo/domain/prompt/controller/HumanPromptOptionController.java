package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptOptionSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptOptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 인간 이미지 프롬프트 선택지 CUD API 컨트롤러 — {@code /api/human-prompts/options}
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Tag(name = "HumanPromptOption", description = "인간 이미지 프롬프트 선택지 관리 API")
@Validated
@RestController
@RequestMapping("/api/human-prompts/options")
@RequiredArgsConstructor
public class HumanPromptOptionController {

    private final HumanPromptOptionService humanPromptOptionService;

    @Operation(summary = "선택지 생성", description = "MANAGER 이상 — attrId에 해당하는 세부항목이 존재해야 한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "선택지 생성 성공")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 / 존재하지 않는 세부항목")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public CommonResponse<HumanPromptOptionSimpleRespDto> createOption(
            @RequestBody @Valid HumanPromptOptionCreateReqDto dto) {
        return CommonResponse.success(humanPromptOptionService.createOption(dto));
    }

    @Operation(summary = "선택지 수정", description = "MANAGER 이상 — name, promptValue, sortOrder 수정 가능.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 선택지")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CommonResponse<HumanPromptOptionSimpleRespDto> updateOption(
            @PathVariable @Positive Long id,
            @RequestBody @Valid HumanPromptOptionUpdateReqDto dto) {
        return CommonResponse.success(humanPromptOptionService.updateOption(id, dto));
    }

    @Operation(summary = "선택지 삭제", description = "MANAGER 이상 — 프롬프트 이력에서 사용 중이면 삭제 거부.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 선택지 / 이력 사용 중")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteOption(@PathVariable @Positive Long id) {
        humanPromptOptionService.deleteOption(id);
        return CommonResponse.success();
    }
}

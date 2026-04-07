package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptAttrSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptAttrService;
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
 * 인간 이미지 프롬프트 세부항목 CUD API 컨트롤러 — {@code /api/human-prompts/attrs}
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Tag(name = "HumanPromptAttr", description = "인간 이미지 프롬프트 세부항목 관리 API")
@Validated
@RestController
@RequestMapping("/api/human-prompts/attrs")
@RequiredArgsConstructor
public class HumanPromptAttrController {

    private final HumanPromptAttrService humanPromptAttrService;

    @Operation(summary = "세부항목 생성", description = "MANAGER 이상 — stepId에 해당하는 단계가 존재해야 한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "세부항목 생성 성공")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 / 존재하지 않는 단계")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public CommonResponse<HumanPromptAttrSimpleRespDto> createAttr(
            @RequestBody @Valid HumanPromptAttrCreateReqDto dto) {
        return CommonResponse.success(humanPromptAttrService.createAttr(dto));
    }

    @Operation(summary = "세부항목 수정", description = "MANAGER 이상 — name, isMultiSelect, isRequired, sortOrder 수정 가능.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 세부항목")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CommonResponse<HumanPromptAttrSimpleRespDto> updateAttr(
            @PathVariable @Positive Long id,
            @RequestBody @Valid HumanPromptAttrUpdateReqDto dto) {
        return CommonResponse.success(humanPromptAttrService.updateAttr(id, dto));
    }

    @Operation(summary = "세부항목 삭제", description = "MANAGER 이상 — 하위 선택지(Option)가 존재하면 삭제 거부.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 세부항목 / 하위 선택지 존재")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteAttr(@PathVariable @Positive Long id) {
        humanPromptAttrService.deleteAttr(id);
        return CommonResponse.success();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 인간 이미지 프롬프트 API 컨트롤러 — {@code /api/human-prompts}
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Tag(name = "HumanPrompt", description = "인간 이미지 전용 프롬프트 생성 API")
@RestController
@RequestMapping("/api/human-prompts")
@RequiredArgsConstructor
public class HumanPromptController {

    private final HumanPromptService humanPromptService;

    @Operation(
            summary = "인간 이미지 단계 구조 조회",
            description = "프롬프트 생성 화면 Tab 2에 필요한 5단계 → 세부항목 → 선택지 전체 구조를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/steps")
    public CommonResponse<List<HumanPromptStepRespDto>> getSteps() {
        return CommonResponse.success(humanPromptService.getSteps());
    }

    @Operation(
            summary = "인간 이미지 프롬프트 생성",
            description = "5단계 구조에서 선택된 옵션 기반으로 Gemini API를 통해 영문 인간 이미지 프롬프트를 생성한다. " +
                          "히스토리 저장 없음.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "프롬프트 생성 성공")
    @ApiResponse(responseCode = "400", description = "선택된 옵션 없음 / 유효하지 않은 입력값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public CommonResponse<String> createPrompt(@RequestBody @Valid HumanPromptCreateReqDto dto) {
        return CommonResponse.success(humanPromptService.createPrompt(dto));
    }
}

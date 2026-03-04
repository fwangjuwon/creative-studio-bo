package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.ImageGenerateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.ImageGenerateRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            description = "5단계 구조에서 선택된 옵션 기반으로 Gemini API를 통해 영문 인간 이미지 프롬프트를 생성하고 이력을 저장한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "프롬프트 생성 성공")
    @ApiResponse(responseCode = "400", description = "선택된 옵션 없음 / 유효하지 않은 입력값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public CommonResponse<PromptHistoryRespDto> createPrompt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid HumanPromptCreateReqDto dto) {
        return CommonResponse.success(humanPromptService.createPrompt(userDetails.getId(), dto));
    }

    @Operation(
            summary = "인간 이미지 생성 (NanaBanana)",
            description = "영문 프롬프트 텍스트를 NanaBanana(gemini-3-pro-image-preview)에 전달하여 이미지를 생성하고 " +
                          "서버에 저장 후 접근 URL을 반환한다. historyId를 전달하면 이력의 imageUrl도 업데이트된다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "이미지 생성 성공")
    @ApiResponse(responseCode = "400", description = "promptText 누락")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/generate-image")
    public CommonResponse<ImageGenerateRespDto> generateImage(@RequestBody @Valid ImageGenerateReqDto dto) {
        String imageUrl = humanPromptService.generateImage(dto.getHistoryId(), dto.getPromptText());
        return CommonResponse.success(ImageGenerateRespDto.builder().imageUrl(imageUrl).build());
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.PromptService;
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
 * Prompt API 컨트롤러 — {@code /api/prompts}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Prompt", description = "프롬프트 생성 및 히스토리 조회 API")
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @Operation(
            summary = "프롬프트 생성",
            description = "카테고리와 태그를 기반으로 Gemini API를 통해 영문 이미지 생성 프롬프트를 생성한다. " +
                          "일일 한도 초과 시 400 에러 응답. USER 이상 모든 권한 허용.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "프롬프트 생성 성공")
    @ApiResponse(responseCode = "400", description = "일일 한도 초과 / 유효하지 않은 입력값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public CommonResponse<PromptHistoryRespDto> createPrompt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PromptCreateReqDto dto) {
        return CommonResponse.success(promptService.createPrompt(userDetails.getId(), dto));
    }

    @Operation(
            summary = "프롬프트 히스토리 조회",
            description = "요청 사용자의 권한에 따라 히스토리 범위가 다르다. " +
                          "USER → 같은 팀 히스토리만, MANAGER/ADMIN → 전체.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public CommonResponse<List<PromptHistoryRespDto>> getPromptHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return CommonResponse.success(promptService.getPromptHistories(userDetails.getId()));
    }
}

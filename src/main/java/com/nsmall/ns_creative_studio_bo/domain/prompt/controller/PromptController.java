package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptHistorySearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.ImageGenerateRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Prompt API 컨트롤러 — {@code /api/prompts}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Prompt", description = "프롬프트 생성 및 히스토리 조회 API")
@Validated
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @Operation(
            summary = "상품 프롬프트 생성",
            description = "상품 카테고리와 태그를 기반으로 Gemini API를 통해 영문 이미지 생성 프롬프트를 생성한다. " +
                          "일일 한도 초과 시 400 에러 응답. USER 이상 모든 권한 허용.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "상품 프롬프트 생성 성공")
    @ApiResponse(responseCode = "400", description = "일일 한도 초과 / 유효하지 않은 입력값")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/goods")
    public CommonResponse<PromptHistoryRespDto> createGoodsPrompt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid GoodsPromptCreateReqDto dto) {
        return CommonResponse.success(promptService.createGoodsPrompt(userDetails.getId(), dto));
    }

    @Operation(
            summary = "상품 이미지 생성 (NanoBanana)",
            description = "영문 프롬프트 텍스트를 NanoBanana(gemini-3-pro-image-preview)에 전달하여 이미지를 생성하고 " +
                          "서버에 저장 후 접근 URL을 반환한다. historyId를 전달하면 이력의 imageUrl도 업데이트된다.",

            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "이미지 생성 성공")
    @ApiResponse(responseCode = "400", description = "promptText 누락 또는 빈 문자열 / 허용되지 않는 MIME 타입 (jpeg, png, webp만 허용) / 파일 크기 15MB 초과")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/goods/generate-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<ImageGenerateRespDto> generateGoodsImage(
            @RequestParam(required = false) Long historyId,
            @NotBlank @RequestParam String promptText,
            @RequestParam(required = false) MultipartFile referenceImage) {
        List<String> imageUrls = promptService.generateGoodsImage(historyId, promptText, referenceImage);
        return CommonResponse.success(ImageGenerateRespDto.builder().imageUrls(imageUrls).build());
    }

    @Operation(
            summary = "프롬프트 히스토리 조회",
            description = "Role 구분 없이 전체 히스토리를 검색 조건 + 페이지 단위로 조회한다. " +
                          "모든 검색 조건은 선택 사항(null이면 전체 조회). " +
                          "loginId: 생성자 필터, promptType: PRODUCT|PERSON, " +
                          "promptStyle: NANO_BANANA|MID_JOURNEY, " +
                          "dateFrom/dateTo: 생성일 범위(포함). " +
                          "pageNum(기본 1), pageSize(기본 10). 정렬 기준: 등록일시 내림차순.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public CommonResponse<PageRespDto<PromptHistoryRespDto>> getPromptHistories(
            @ModelAttribute @Valid PromptHistorySearchReqDto searchReqDto) {
        return CommonResponse.success(promptService.getPromptHistories(searchReqDto));
    }

    @Operation(
            summary = "프롬프트 이력 삭제",
            description = "지정한 프롬프트 이력을 삭제한다. ADMIN 전용. " +
                          "매핑 테이블(goods_prompt_history_tag 또는 human_prompt_history_option) 삭제 후 " +
                          "prompt_history를 삭제하며, 연결된 이미지 파일도 함께 삭제한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "이력 삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 이력 ID")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteHistory(@PathVariable @Positive Long id) {
        promptService.deleteHistory(id);
        return CommonResponse.success();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.common.security.CustomUserDetails;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.ImageGenerateRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.FusionImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 합성 이미지 생성 API 컨트롤러 — {@code /api/prompts/fusion}
 *
 * @author gaeddo
 * @since 2026-05-19
 */
@Tag(name = "FusionImage", description = "상품+인물 합성 이미지 생성 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@RestController
@RequestMapping("/api/prompts/fusion")
@RequiredArgsConstructor
public class FusionImageController {

    private final FusionImageService fusionImageService;

    @Operation(
            summary = "합성 이미지 생성 (NanoBanana)",
            description = "상품 이미지와 인물 이미지를 NanoBanana(gemini-3-pro-image-preview)로 합성하여 " +
                          "이미지를 생성하고 접근 URL 목록을 반환한다. " +
                          "허용 MIME 타입: jpeg, png, webp / 최대 파일 크기: 15MB"
    )
    @ApiResponse(responseCode = "200", description = "합성 이미지 생성 성공")
    @ApiResponse(responseCode = "400", description = "이미지 누락 / 허용되지 않는 MIME 타입 / 파일 크기 15MB 초과")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PostMapping(value = "/generate-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<ImageGenerateRespDto> generateFusionImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam MultipartFile productImage,
            @RequestParam MultipartFile personImage,
            @RequestParam(required = false) String pose,
            @RequestParam(required = false) String instruction,
            @RequestParam(required = false) String imageAspectRatio) {

        List<String> imageUrls = fusionImageService.generateFusionImages(
                userDetails.getId(), productImage, personImage, pose, instruction, imageAspectRatio);

        return CommonResponse.success(ImageGenerateRespDto.builder().imageUrls(imageUrls).build());
    }
}

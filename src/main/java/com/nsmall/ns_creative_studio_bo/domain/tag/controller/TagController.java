package com.nsmall.ns_creative_studio_bo.domain.tag.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagService;
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

import java.util.List;

/**
 * Tag API 컨트롤러 — {@code /api/tags}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Tag", description = "태그 관리 API")
@Validated
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 생성", description = "MANAGER 이상",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "태그 생성 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public CommonResponse<TagRespDto> createTag(@RequestBody @Valid TagCreateReqDto dto) {
        return CommonResponse.success(tagService.createTag(dto));
    }

    @Operation(summary = "태그 단건 조회", description = "인증된 모든 사용자 허용 — 존재하지 않는 id면 200 + data: null",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public CommonResponse<TagRespDto> getTag(@PathVariable @Positive Long id) {
        return CommonResponse.success(tagService.getTag(id).orElse(null));
    }

    @Operation(
            summary = "태그 목록 조회",
            description = "tagGroupId 미입력 → 전체 태그 목록, tagGroupId 입력 → 해당 그룹의 태그 목록. 인증된 모든 사용자 허용.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public CommonResponse<List<TagRespDto>> getTags(
            @RequestParam(required = false) Long tagGroupId) {
        return CommonResponse.success(tagService.getTags(tagGroupId));
    }

    @Operation(summary = "태그 수정", description = "MANAGER 이상 — name, sortOrder 수정 가능",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CommonResponse<TagRespDto> updateTag(
            @PathVariable @Positive Long id,
            @RequestBody @Valid TagUpdateReqDto dto) {
        return CommonResponse.success(tagService.updateTag(id, dto));
    }

    @Operation(summary = "태그 삭제", description = "MANAGER 이상",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteTag(@PathVariable @Positive Long id) {
        tagService.deleteTag(id);
        return CommonResponse.success();
    }
}

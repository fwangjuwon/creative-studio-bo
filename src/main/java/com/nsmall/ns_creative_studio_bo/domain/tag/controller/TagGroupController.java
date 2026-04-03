package com.nsmall.ns_creative_studio_bo.domain.tag.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagGroupService;
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
 * TagGroup API 컨트롤러 — {@code /api/tag-groups}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "TagGroup", description = "태그 그룹 관리 API")
@Validated
@RestController
@RequestMapping("/api/tag-groups")
@RequiredArgsConstructor
public class TagGroupController {

    private final TagGroupService tagGroupService;

    @Operation(summary = "태그 그룹 생성", description = "MANAGER 이상",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "태그 그룹 생성 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public CommonResponse<TagGroupRespDto> createTagGroup(@RequestBody @Valid TagGroupCreateReqDto dto) {
        return CommonResponse.success(tagGroupService.createTagGroup(dto));
    }

    @Operation(summary = "태그 그룹 단건 조회", description = "인증된 모든 사용자 허용 — 존재하지 않는 id면 200 + data: null",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public CommonResponse<TagGroupRespDto> getTagGroup(@PathVariable @Positive Long id) {
        return CommonResponse.success(tagGroupService.getTagGroup(id).orElse(null));
    }

    @Operation(summary = "태그 그룹 전체 목록 조회", description = "인증된 모든 사용자 허용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public CommonResponse<List<TagGroupRespDto>> getTagGroups() {
        return CommonResponse.success(tagGroupService.getTagGroups());
    }

    @Operation(summary = "태그 그룹 수정", description = "MANAGER 이상 — name, isMultiSelect, sortOrder 수정 가능",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CommonResponse<TagGroupRespDto> updateTagGroup(
            @PathVariable @Positive Long id,
            @RequestBody @Valid TagGroupUpdateReqDto dto) {
        return CommonResponse.success(tagGroupService.updateTagGroup(id, dto));
    }

    @Operation(summary = "태그 그룹 삭제", description = "MANAGER 이상",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteTagGroup(@PathVariable @Positive Long id) {
        tagGroupService.deleteTagGroup(id);
        return CommonResponse.success();
    }
}

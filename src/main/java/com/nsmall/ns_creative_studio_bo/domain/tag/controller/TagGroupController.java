package com.nsmall.ns_creative_studio_bo.domain.tag.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TagGroup API 컨트롤러 — {@code /api/tag-groups}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "TagGroup", description = "태그 그룹 관리 API")
@RestController
@RequestMapping("/api/tag-groups")
@RequiredArgsConstructor
public class TagGroupController {

    private final TagGroupService tagGroupService;

    @Operation(summary = "태그 그룹 생성", description = "ADMIN / MANAGER 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "태그 그룹 생성 성공")
    @PostMapping
    public CommonResponse<TagGroupRespDto> createTagGroup(@RequestBody @Valid TagGroupCreateReqDto dto) {
        return CommonResponse.success(tagGroupService.createTagGroup(dto));
    }

    @Operation(summary = "태그 그룹 단건 조회", description = "존재하지 않는 id면 200 + data: null 반환")
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @GetMapping("/{id}")
    public CommonResponse<TagGroupRespDto> getTagGroup(@PathVariable Long id) {
        return CommonResponse.success(tagGroupService.getTagGroup(id).orElse(null));
    }

    @Operation(summary = "태그 그룹 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @GetMapping
    public CommonResponse<List<TagGroupRespDto>> getTagGroups() {
        return CommonResponse.success(tagGroupService.getTagGroups());
    }

    @Operation(summary = "태그 그룹 수정", description = "ADMIN / MANAGER 전용 — name, isMultiSelect, sortOrder 수정 가능")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public CommonResponse<TagGroupRespDto> updateTagGroup(
            @PathVariable Long id,
            @RequestBody TagGroupUpdateReqDto dto) {
        return CommonResponse.success(tagGroupService.updateTagGroup(id, dto));
    }

    @Operation(summary = "태그 그룹 삭제", description = "ADMIN / MANAGER 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteTagGroup(@PathVariable Long id) {
        tagGroupService.deleteTagGroup(id);
        return CommonResponse.success();
    }
}

package com.nsmall.ns_creative_studio_bo.domain.tag.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tag API 컨트롤러 — {@code /api/tags}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Tag", description = "태그 관리 API")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 생성", description = "ADMIN / MANAGER 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "태그 생성 성공")
    @PostMapping
    public CommonResponse<TagRespDto> createTag(@RequestBody @Valid TagCreateReqDto dto) {
        return CommonResponse.success(tagService.createTag(dto));
    }

    @Operation(summary = "태그 단건 조회", description = "존재하지 않는 id면 200 + data: null 반환")
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @GetMapping("/{id}")
    public CommonResponse<TagRespDto> getTag(@PathVariable Long id) {
        return CommonResponse.success(tagService.getTag(id).orElse(null));
    }

    @Operation(
            summary = "태그 목록 조회",
            description = "tagGroupId 미입력 → 전체 태그 목록, tagGroupId 입력 → 해당 그룹의 태그 목록"
    )
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @GetMapping
    public CommonResponse<List<TagRespDto>> getTags(
            @RequestParam(required = false) Long tagGroupId) {
        return CommonResponse.success(tagService.getTags(tagGroupId));
    }

    @Operation(summary = "태그 수정", description = "ADMIN / MANAGER 전용 — name, sortOrder 수정 가능")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public CommonResponse<TagRespDto> updateTag(
            @PathVariable Long id,
            @RequestBody TagUpdateReqDto dto) {
        return CommonResponse.success(tagService.updateTag(id, dto));
    }

    @Operation(summary = "태그 삭제", description = "ADMIN / MANAGER 전용 — Step 4 Security 적용 후 접근 제어 추가 예정")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return CommonResponse.success();
    }
}

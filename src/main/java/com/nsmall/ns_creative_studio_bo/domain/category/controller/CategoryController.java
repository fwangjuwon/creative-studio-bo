package com.nsmall.ns_creative_studio_bo.domain.category.controller;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.service.CategoryService;
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
 * Category API 컨트롤러 — {@code /api/categories}
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 생성", description = "MANAGER 이상 — MANAGER, ADMIN 허용",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "카테고리 생성 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public CommonResponse<CategoryRespDto> createCategory(@RequestBody @Valid CategoryCreateReqDto dto) {
        return CommonResponse.success(categoryService.createCategory(dto));
    }

    @Operation(summary = "카테고리 단건 조회", description = "인증된 모든 사용자 허용 — 존재하지 않는 id면 200 + data: null",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "조회 성공 (미존재 시 data: null)")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public CommonResponse<CategoryRespDto> getCategory(@PathVariable Long id) {
        return CommonResponse.success(categoryService.getCategory(id).orElse(null));
    }

    @Operation(
            summary = "카테고리 목록 조회 (드릴다운)",
            description = "parentId 미입력 → LCS(대분류) 전체 목록, parentId 입력 → 해당 카테고리의 하위 목록. 인증된 모든 사용자 허용.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public CommonResponse<List<CategoryRespDto>> getCategories(
            @RequestParam(required = false) Long parentId) {
        return CommonResponse.success(categoryService.getCategories(parentId));
    }

    @Operation(summary = "카테고리 수정", description = "MANAGER 이상 — name, sortOrder만 수정 가능",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public CommonResponse<CategoryRespDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryUpdateReqDto dto) {
        return CommonResponse.success(categoryService.updateCategory(id, dto));
    }

    @Operation(summary = "카테고리 삭제", description = "MANAGER 이상",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (MANAGER 이상)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return CommonResponse.success();
    }
}

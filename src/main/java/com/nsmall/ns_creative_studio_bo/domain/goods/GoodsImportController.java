package com.nsmall.ns_creative_studio_bo.domain.goods;

import com.nsmall.ns_creative_studio_bo.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goods")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Goods import", description = "NS몰 상품 이미지 가져오기")
public class GoodsImportController {
    private final NsmallGoodsService service;

    @GetMapping("/{goodsCd}/images")
    @Operation(summary = "상품번호로 대표·상세 이미지 조회")
    public CommonResponse<GoodsImages> images(@PathVariable String goodsCd) {
        return CommonResponse.success(service.getImages(goodsCd));
    }

    @GetMapping("/{goodsCd}/images/{imageId}")
    @Operation(summary = "조회한 상품 이미지의 원본 바이트 가져오기")
    public ResponseEntity<byte[]> image(@PathVariable String goodsCd, @PathVariable String imageId) {
        NsmallGoodsService.ImageData image = service.getImage(goodsCd, imageId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(image.bytes());
    }
}

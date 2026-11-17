package com.nsmall.ns_creative_studio_bo.domain.goods;

import java.util.List;

public record GoodsImages(String goodsCd, String productName, String productUrl, List<Image> images,
                          int unsupportedDetailImageCount) {
    public record Image(String id, String kind, String label, String sourceUrl) {}
}

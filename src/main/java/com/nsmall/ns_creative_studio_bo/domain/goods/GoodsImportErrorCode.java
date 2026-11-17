package com.nsmall.ns_creative_studio_bo.domain.goods;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoodsImportErrorCode implements ErrorCode {
    INVALID_GOODS_CODE("GI001", "올바른 상품번호를 입력해 주세요.", 400),
    GOODS_NOT_FOUND("GI002", "상품을 찾을 수 없습니다. 상품번호와 판매 상태를 확인해 주세요.", 404),
    NO_IMAGES("GI003", "가져올 수 있는 상품 이미지가 없습니다.", 404),
    UPSTREAM_FAILED("GI004", "NS몰 상품 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.", 502),
    INVALID_IMAGE("GI005", "상품 이미지를 불러올 수 없습니다. 다른 이미지를 선택해 주세요.", 422),
    IMAGE_TOO_LARGE("GI006", "원본 이미지가 너무 큽니다. 다른 이미지를 선택해 주세요. (최대 32MB)", 422);

    private final String code;
    private final String message;
    private final int httpStatus;
}

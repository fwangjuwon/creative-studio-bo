package com.nsmall.ns_creative_studio_bo.domain.category;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Category 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND("C001", "카테고리를 찾을 수 없습니다.", 400),
    PARENT_CATEGORY_NOT_FOUND("C002", "상위 카테고리를 찾을 수 없습니다.", 400),
    INVALID_CATEGORY_HIERARCHY("C003", "카테고리 계층 구조가 올바르지 않습니다.", 400),
    CATEGORY_HAS_CHILDREN("C004", "하위 카테고리가 존재하여 삭제할 수 없습니다.", 400),
    CATEGORY_IN_USE("C005", "프롬프트 히스토리에서 사용 중인 카테고리는 삭제할 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

package com.nsmall.ns_creative_studio_bo.domain.tag;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TagGroup 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@RequiredArgsConstructor
public enum TagGroupErrorCode implements ErrorCode {

    TAG_GROUP_NOT_FOUND("TG001", "태그 그룹을 찾을 수 없습니다.", 400),
    TAG_GROUP_HAS_TAGS("TG002", "소속 태그가 존재하여 태그 그룹을 삭제할 수 없습니다.", 400),
    DUPLICATE_SORT_ORDER("TG003", "이미 같은 정렬순서를 가진 태그 그룹이 존재합니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

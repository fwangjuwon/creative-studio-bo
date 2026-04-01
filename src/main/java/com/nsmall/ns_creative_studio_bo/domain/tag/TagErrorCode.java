package com.nsmall.ns_creative_studio_bo.domain.tag;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tag 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements ErrorCode {

    TAG_NOT_FOUND("T001", "태그를 찾을 수 없습니다.", 400),
    TAG_GROUP_NOT_FOUND("T002", "태그가 속한 그룹을 찾을 수 없습니다.", 400),
    TAG_IN_USE("T003", "사용 중인 태그는 삭제할 수 없습니다.", 400),
    DUPLICATE_SORT_ORDER("T004", "동일 그룹 내에 이미 같은 정렬순서를 가진 태그가 존재합니다.", 400),
    DUPLICATE_TAG_NAME("T004", "이미 존재하는 태그명입니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

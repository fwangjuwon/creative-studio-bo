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
    TAG_GROUP_NOT_FOUND("T002", "태그가 속한 그룹을 찾을 수 없습니다.", 400);

    private final String code;
    private final String message;
    private final int httpStatus;
}

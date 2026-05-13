package com.nsmall.ns_creative_studio_bo.domain.auth;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * NsistEmpMapping 도메인 에러 코드
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Getter
@RequiredArgsConstructor
public enum NsistEmpMappingErrorCode implements ErrorCode {

    DUPLICATE_EMP_CD("NE001", "이미 등록된 사원번호입니다.", HttpStatus.BAD_REQUEST.value()),
    MAPPING_NOT_FOUND("NE002", "매핑 정보를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    USER_NOT_FOUND_FOR_MAPPING("NE003", "매핑 대상 사용자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}

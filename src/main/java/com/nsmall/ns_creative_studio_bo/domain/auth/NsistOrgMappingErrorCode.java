package com.nsmall.ns_creative_studio_bo.domain.auth;

import com.nsmall.ns_creative_studio_bo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * NsistOrgMapping 도메인 에러 코드
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Getter
@RequiredArgsConstructor
public enum NsistOrgMappingErrorCode implements ErrorCode {

    DUPLICATE_ORG_CD("NO001", "이미 등록된 조직코드입니다.", HttpStatus.BAD_REQUEST.value()),
    MAPPING_NOT_FOUND("NO002", "매핑 정보를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    USER_NOT_FOUND_FOR_MAPPING("NO003", "매핑 대상 사용자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}

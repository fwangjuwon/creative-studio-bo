package com.nsmall.ns_creative_studio_bo.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NsistOrgMapping 응답 DTO — 매핑 목록 조회 (ADMIN 전용)
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsistOrgMappingRespDto {

    private Long id;

    /** nsist 조직코드 */
    private String orgCd;

    /** 매핑된 NCS 사용자 ID */
    private Long userId;

    /** 매핑된 NCS 로그인 아이디 (user 테이블 JOIN) */
    private String loginId;

    /** 매핑된 NCS 사용자 이름 (user 테이블 JOIN) */
    private String name;

    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;
}

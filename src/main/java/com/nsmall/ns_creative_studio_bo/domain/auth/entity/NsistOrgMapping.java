package com.nsmall.ns_creative_studio_bo.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NsistOrgMapping 엔티티 — {@code nsist_org_mapping} 테이블 매핑
 *
 * <pre>
 * nsist 조직코드(org_cd)와 NCS 팀계정(user_id)의 매핑 정보. USER 역할 계정 전용.
 * org_cd : UNIQUE — 한 조직코드는 하나의 NCS 팀계정에만 매핑된다.
 * 로그인 시 nsist_emp_mapping(emp_cd) 조회 우선, 없으면 이 테이블로 fallback.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsistOrgMapping {

    private Long id;

    /** nsist 조직코드 (UNIQUE) */
    private String orgCd;

    /** 논리 FK → user.id (팀 계정) */
    private Long userId;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

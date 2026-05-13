package com.nsmall.ns_creative_studio_bo.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NsistEmpMapping 엔티티 — {@code nsist_emp_mapping} 테이블 매핑
 *
 * <pre>
 * nsist 사원번호(emp_cd)와 NCS 사용자(user_id)의 매핑 정보.
 * emp_cd : UNIQUE — 한 사번은 하나의 NCS 계정에만 매핑된다.
 * user_id : N:1 — 여러 사번이 하나의 NCS 팀 계정을 공유할 수 있다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsistEmpMapping {

    private Long id;

    /** nsist 사원번호 (UNIQUE) */
    private String empCd;

    /** 논리 FK → user.id */
    private Long userId;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

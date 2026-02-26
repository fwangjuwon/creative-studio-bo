package com.nsmall.ns_creative_studio_bo.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User 엔티티 — {@code user} 테이블 매핑
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String loginId;
    private String password;
    private String name;
    private String role;
    private String teamName;
    private Integer dailyLimit;

    // 감사(Audit) 컬럼 — DB: snake_case → Java: camelCase (MyBatis mapUnderscoreToCamelCase 자동 매핑)
    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

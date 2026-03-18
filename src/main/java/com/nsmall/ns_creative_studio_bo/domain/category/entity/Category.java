package com.nsmall.ns_creative_studio_bo.domain.category.entity;

import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Category 엔티티 — {@code category} 테이블 매핑
 *
 * <pre>
 * 3단계 계층 구조: LCS(대분류) > MCS(중분류) > SCS(소분류)
 * parent_id self-reference — LCS는 parent_id = null
 * SCS가 최종 선택 단계 (프롬프트 생성 시 SCS 카테고리 ID 사용)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    private Long id;
    private String name;
    private CategoryType categoryType;
    private Long parentId;
    private Integer sortOrder;

    // 감사(Audit) 컬럼 — DB: snake_case → Java: camelCase (MyBatis mapUnderscoreToCamelCase 자동 매핑)
    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

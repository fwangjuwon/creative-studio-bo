package com.nsmall.ns_creative_studio_bo.domain.tag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TagGroup 엔티티 — {@code tag_group} 테이블 매핑
 *
 * <pre>
 * 태그 그룹 (예: 인물, 분위기)
 * is_multi_select: 복수 선택 가능 여부 (true = 복수, false = 단일)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagGroup {

    private Long id;
    private String name;
    private Boolean isMultiSelect;
    private Integer sortOrder;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

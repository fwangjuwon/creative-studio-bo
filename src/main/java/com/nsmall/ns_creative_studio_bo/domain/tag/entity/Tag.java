package com.nsmall.ns_creative_studio_bo.domain.tag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tag 엔티티 — {@code tag} 테이블 매핑
 *
 * <pre>
 * 태그 값 (예: 남자, 여자, 화사한)
 * tag_group_id: 논리 FK → TagGroup (DB 제약 없음)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    private Long id;
    private Long tagGroupId;
    private String name;
    private Integer sortOrder;

    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private String finalRvisPic;
    private LocalDateTime finalRvisDttm;
}

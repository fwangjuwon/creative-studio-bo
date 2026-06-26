package com.nsmall.ns_creative_studio_bo.domain.category.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 경로 배치 조회 결과 DTO
 *
 * <pre>
 * ※ 내부 전용 DTO — API 응답에 직접 노출되지 않음.
 *    Mapper → Service 계층 간 배치 조회 결과 전달 목적으로만 사용한다.
 *
 * N+1 방지 목적으로 사용.
 * 여러 SCS category_id에 대해 "LCS > MCS > SCS" 경로 문자열을 한 번에 조회할 때 사용한다.
 * Service에서 id 기준으로 Map 변환 후 조립한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-31
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryPathDto {

    private Long id;
    private String categoryPath;

    /** 상위 계층 ID 체인 (히스토리 → 생성 폼 카테고리 드릴다운 복원용) */
    private Long lcsId;
    private Long mcsId;
    private Long scsId;
}

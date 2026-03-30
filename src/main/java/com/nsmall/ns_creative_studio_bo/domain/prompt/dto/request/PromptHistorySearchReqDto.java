package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import com.nsmall.ns_creative_studio_bo.common.page.PageReqDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 프롬프트 이력 검색 요청 DTO
 *
 * <pre>
 * PageReqDto를 extend하여 페이징 + 검색 파라미터를 하나의 DTO로 통합.
 * 모든 검색 조건은 nullable — null이면 해당 조건 무시(전체 조회).
 *
 * loginId     : 생성자 필터 (init_regi_pic 컬럼 직접 비교, JOIN 없음)
 * promptType  : 상품/인물 구분 ("PRODUCT" | "PERSON")
 * promptStyle : 프롬프트 스타일 ("NANO_BANANA" | "MID_JOURNEY")
 * dateFrom    : 생성일 시작 (포함)
 * dateTo      : 생성일 종료 (포함)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-30
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "프롬프트 이력 검색 요청")
public class PromptHistorySearchReqDto extends PageReqDto {

    @Schema(description = "생성자 loginId 필터 (null이면 전체)", example = "user01")
    private String loginId;

    @Schema(description = "프롬프트 종류 필터 (PRODUCT | PERSON, null이면 전체)", example = "PRODUCT")
    private String promptType;

    @Schema(description = "프롬프트 스타일 필터 (NANO_BANANA | MID_JOURNEY, null이면 전체)", example = "NANO_BANANA")
    private String promptStyle;

    @Schema(description = "생성일 시작 (포함, null이면 제한 없음)", example = "2026-03-01")
    private LocalDate dateFrom;

    @Schema(description = "생성일 종료 (포함, null이면 제한 없음)", example = "2026-03-31")
    private LocalDate dateTo;
}

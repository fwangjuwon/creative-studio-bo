package com.nsmall.ns_creative_studio_bo.common.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 페이징 요청 공통 DTO
 *
 * <pre>
 * Controller에서 페이징 파라미터를 받아 Service로 전달한다.
 * PageHelper.startPage()의 입력으로 사용된다.
 *
 * 기본값: pageNum = 1, pageSize = 10
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이징 요청")
public class PageReqDto {

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer pageNum = 1;

    @Schema(description = "페이지 크기 (1~1000)", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 1000, message = "페이지 크기는 1000 이하여야 합니다")
    private Integer pageSize = 10;
}

package com.nsmall.ns_creative_studio_bo.common.page;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 페이징 응답 공통 DTO (불변)
 *
 * <pre>
 * PageHelper의 PageInfo를 받아 페이징 메타정보와 데이터를 담는다.
 *
 * of(PageInfo)       — 쿼리 결과 타입과 응답 타입이 동일한 경우
 * ofWithItems(...)   — 쿼리 결과를 변환(transform)한 후 별도 items를 주입하는 경우
 *                      (N+1 방지를 위해 추가 조회 후 가공할 때 사용)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-16
 */
@Schema(description = "페이징 응답")
public record PageRespDto<T>(

    @Schema(description = "현재 페이지 번호")
    Integer pageNum,

    @Schema(description = "페이지 크기")
    Integer pageSize,

    @Schema(description = "총 레코드 수")
    Long total,

    @Schema(description = "총 페이지 수")
    Integer pages,

    @Schema(description = "데이터 목록")
    List<T> items,

    @Schema(description = "이전 페이지 존재 여부")
    Boolean hasPreviousPage,

    @Schema(description = "다음 페이지 존재 여부")
    Boolean hasNextPage
) {
    /**
     * PageHelper의 PageInfo로부터 PageRespDto 생성.
     * 쿼리 결과 타입과 응답 타입이 동일한 단순 케이스에 사용한다.
     *
     * @param pageInfo PageHelper PageInfo 객체
     * @param <T>      데이터 타입
     * @return PageRespDto 객체
     */
    public static <T> PageRespDto<T> of(PageInfo<T> pageInfo) {
        return new PageRespDto<>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages(),
                pageInfo.getList(),
                pageInfo.isHasPreviousPage(),
                pageInfo.isHasNextPage()
        );
    }

    /**
     * PageHelper의 PageInfo에서 메타정보를 가져오고, items는 외부에서 주입하는 PageRespDto 생성.
     *
     * <pre>
     * 쿼리 결과(S 타입)를 변환(transform)하여 응답 타입(T)으로 반환해야 하는 경우에 사용한다.
     * PageHelper가 selectList()를 페이징하여 COUNT 및 총건수를 확보한 뒤,
     * 각 레코드에 추가 조회(N+1 케이스)를 수행하고 변환한 items를 넘기면
     * 올바른 메타정보(total, pages 등)를 유지하면서 변환된 데이터를 담을 수 있다.
     * </pre>
     *
     * @param pageInfo PageHelper PageInfo 객체 (메타정보 소스)
     * @param items    변환된 데이터 목록
     * @param <S>      PageInfo의 원본 데이터 타입
     * @param <T>      변환 후 응답 데이터 타입
     * @return PageRespDto 객체
     */
    public static <S, T> PageRespDto<T> ofWithItems(PageInfo<S> pageInfo, List<T> items) {
        return new PageRespDto<>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages(),
                items,
                pageInfo.isHasPreviousPage(),
                pageInfo.isHasNextPage()
        );
    }
}

package com.nsmall.ns_creative_studio_bo.common.page;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Supplier;

/**
 * PageHelper 공통 유틸리티
 *
 * <pre>
 * PageHelper의 try-with-resources 처리를 공통화한다.
 * Service에서 간단하게 페이징 쿼리를 실행할 수 있도록 지원한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-16
 */
public class PageHelperUtil {

    private PageHelperUtil() {}

    /**
     * 페이징 쿼리 실행 (try-with-resources 자동 처리)
     *
     * <pre>
     * Supplier를 사용하는 이유:
     * - MyBatis 쿼리 실행을 지연(defer)시켜 PageHelper 설정 후에 실행하기 위함
     * - List를 직접 받으면 이미 쿼리가 실행된 후이므로 페이징 적용 불가
     * - Supplier로 "실행 방법"만 전달하고, PageHelper 설정 완료 후 get() 호출
     *
     * {@link PageHelper#startPage}를 try-with-resources로 감싸서
     * 예외 발생 시에도 ThreadLocal이 확실히 정리되도록 보장한다.
     *
     * 정렬 조건은 MyBatis XML의 ORDER BY 절에서 명시적으로 관리한다.
     * </pre>
     *
     * @param pageReqDto    페이징 요청 정보
     * @param queryExecutor 쿼리 실행 함수 (람다: () -> mapper.selectXxx(...))
     * @param <T>           결과 데이터 타입
     * @return PageInfo 객체
     */
    public static <T> PageInfo<T> pagedQuery(PageReqDto pageReqDto, Supplier<List<T>> queryExecutor) {
        try (Page<?> ignored = PageHelper.startPage(pageReqDto.getPageNum(), pageReqDto.getPageSize())) {
            List<T> list = queryExecutor.get();
            return new PageInfo<>(list);
        }
        // try-with-resources에 의해 자동으로 Page.close() 호출 → ThreadLocal 정리 보장
    }

    /**
     * 페이징 쿼리 실행 후 PageRespDto로 변환
     *
     * <pre>
     * pagedQuery()를 호출하고 결과를 PageRespDto로 변환한다.
     * 쿼리 결과 타입과 응답 타입이 동일한 단순 케이스에 사용한다.
     *
     * 사용 예시:
     * return PageHelperUtil.pagedResponse(pageReqDto,
     *     () -> goodsMapper.selectList(searchDto));
     * </pre>
     *
     * @param pageReqDto    페이징 요청 정보
     * @param queryExecutor 쿼리 실행 함수 (람다: () -> mapper.selectXxx(...))
     * @param <T>           결과 데이터 타입
     * @return PageRespDto 객체
     */
    public static <T> PageRespDto<T> pagedResponse(PageReqDto pageReqDto, Supplier<List<T>> queryExecutor) {
        PageInfo<T> pageInfo = pagedQuery(pageReqDto, queryExecutor);
        return PageRespDto.of(pageInfo);
    }
}

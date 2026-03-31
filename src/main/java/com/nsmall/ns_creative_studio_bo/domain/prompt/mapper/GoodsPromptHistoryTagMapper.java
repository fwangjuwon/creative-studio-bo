package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.GoodsTagDetailDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.GoodsPromptHistoryTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * GoodsPromptHistoryTag MyBatis Mapper 인터페이스 — 상품 프롬프트-태그 매핑 (상품 전용)
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface GoodsPromptHistoryTagMapper {

    /**
     * 여러 상품 프롬프트-태그 매핑 레코드를 한 번에 삽입 (batch insert)
     */
    void insertBatch(@Param("list") List<GoodsPromptHistoryTag> list);

    /**
     * 특정 프롬프트 히스토리에 매핑된 태그 ID 목록 조회
     */
    List<Long> selectTagIdsByPromptHistoryId(@Param("promptHistoryId") Long promptHistoryId);

    /**
     * 특정 프롬프트 히스토리에 매핑된 태그명 목록 조회 (이력 화면 표시용)
     */
    List<String> selectTagNamesByPromptHistoryId(@Param("promptHistoryId") Long promptHistoryId);

    /**
     * 여러 프롬프트 히스토리 ID에 대한 태그 상세 정보 배치 조회 (N+1 방지용)
     *
     * <pre>
     * tagIds + tagNames 를 단일 IN 쿼리로 한 번에 조회한다.
     * Service에서 promptHistoryId 기준으로 groupingBy 후 조립한다.
     * </pre>
     */
    List<GoodsTagDetailDto> selectTagDetailsByPromptHistoryIds(@Param("ids") List<Long> ids);

    /**
     * 해당 태그가 상품 프롬프트 히스토리에서 사용 중인지 여부 조회 (삭제 방어용)
     */
    boolean existsByTagId(@Param("tagId") Long tagId);
}

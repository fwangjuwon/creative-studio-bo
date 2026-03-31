package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanOptionDetailDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptHistoryOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * HumanPromptHistoryOption MyBatis Mapper 인터페이스 — 인물 프롬프트-선택옵션 매핑 (인물 전용)
 *
 * @author jjhoo8936
 * @since 2026-03-09
 */
@Mapper
public interface HumanPromptHistoryOptionMapper {

    boolean existsByOptionId(@Param("optionId") Long optionId);

    /**
     * 여러 인물 프롬프트-옵션 매핑 레코드를 한 번에 삽입 (batch insert)
     */
    void insertBatch(@Param("list") List<HumanPromptHistoryOption> list);

    /**
     * 특정 프롬프트 히스토리에 매핑된 옵션 한국어명 목록 조회 (이력 화면 표시용)
     */
    List<String> selectOptionNamesByHistoryId(@Param("promptHistoryId") Long promptHistoryId);

    /**
     * 여러 프롬프트 히스토리 ID에 대한 옵션 상세 정보 배치 조회 (N+1 방지용)
     *
     * <pre>
     * 단계 순서(step_number → attr sort_order → option sort_order)를 유지하며 조회한다.
     * Service에서 promptHistoryId 기준으로 groupingBy 후 조립한다.
     * </pre>
     */
    List<HumanOptionDetailDto> selectOptionDetailsByHistoryIds(@Param("ids") List<Long> ids);
}

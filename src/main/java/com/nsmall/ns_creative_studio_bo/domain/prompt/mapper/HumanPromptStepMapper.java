package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 인간 이미지 프롬프트 단계 MyBatis Mapper
 *
 * <pre>
 * human_prompt_step + human_prompt_attr + human_prompt_option 3테이블 LEFT JOIN 조회.
 * ResultMap + collection으로 중첩 DTO 매핑.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Mapper
public interface HumanPromptStepMapper {

    /**
     * 전체 단계 + 세부항목 + 선택지를 한 번에 조회한다.
     * step_number ASC, attr.sort_order ASC, option.sort_order ASC 정렬.
     *
     * @return 단계 응답 DTO 목록 (attrs, options 중첩 포함)
     */
    List<HumanPromptStepRespDto> selectAllWithAttrsAndOptions();
}

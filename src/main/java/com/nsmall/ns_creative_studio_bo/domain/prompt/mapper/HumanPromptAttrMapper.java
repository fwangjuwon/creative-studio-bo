package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 인간 이미지 프롬프트 세부항목 MyBatis Mapper
 *
 * <pre>
 * human_prompt_attr 테이블 매핑.
 * Loop 1: Step 삭제 시 하위 Attr 존재 여부 확인용 메서드 포함.
 * Loop 2: selectById, selectList, insert, update, deleteById 추가 예정.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Mapper
public interface HumanPromptAttrMapper {

    boolean existsByStepId(@Param("stepId") Long stepId);
}

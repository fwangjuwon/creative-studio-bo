package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptAttr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 인간 이미지 프롬프트 세부항목 MyBatis Mapper
 *
 * <pre>
 * human_prompt_attr 테이블 매핑.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@Mapper
public interface HumanPromptAttrMapper {

    Optional<HumanPromptAttr> selectById(@Param("id") Long id);

    /**
     * 필수(is_required=true) 세부항목 ID 목록을 조회한다.
     * 프롬프트 생성 시 필수 항목 선택 여부 검증에 사용.
     *
     * @return 필수 attr ID 목록
     */
    List<Long> selectRequiredAttrIds();

    List<HumanPromptAttr> selectList(@Param("stepId") Long stepId);

    boolean existsByStepId(@Param("stepId") Long stepId);

    void insert(HumanPromptAttr attr);

    void update(HumanPromptAttr attr);

    void deleteById(@Param("id") Long id);
}

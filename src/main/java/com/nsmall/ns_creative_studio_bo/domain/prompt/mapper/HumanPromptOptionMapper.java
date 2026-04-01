package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 인간 이미지 프롬프트 선택지 MyBatis Mapper
 *
 * <pre>
 * 프롬프트 생성 요청 시 선택된 option ID 목록으로 option 정보 + step_number 조회.
 * service에서 stepNumber 기준 그룹핑하여 Gemini user message 구성에 사용.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-03
 */
@Mapper
public interface HumanPromptOptionMapper {

    Optional<HumanPromptOption> selectById(@Param("id") Long id);

    List<HumanPromptOption> selectList(@Param("attrId") Long attrId);

    boolean existsByAttrId(@Param("attrId") Long attrId);

    boolean existsByNameAndAttrId(@Param("name") String name, @Param("attrId") Long attrId, @Param("excludeId") Long excludeId);

    void insert(HumanPromptOption option);

    void update(HumanPromptOption option);

    void deleteById(@Param("id") Long id);

    /**
     * 선택된 option ID 목록으로 option 정보 + step_number를 조회한다.
     *
     * @param ids 선택된 option ID 목록
     * @return option 목록 (stepNumber 포함)
     */
    List<HumanPromptOption> selectByIds(@Param("ids") List<Long> ids);
}

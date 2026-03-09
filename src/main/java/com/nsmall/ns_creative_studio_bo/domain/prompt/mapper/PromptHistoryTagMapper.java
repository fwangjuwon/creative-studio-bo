package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistoryTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PromptHistoryTag MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface PromptHistoryTagMapper {

    /**
     * 여러 프롬프트-태그 매핑 레코드를 한 번에 삽입 (batch insert)
     */
    void insertBatch(@Param("list") List<PromptHistoryTag> list);

    /**
     * 특정 프롬프트 히스토리에 매핑된 태그 ID 목록 조회
     */
    List<Long> selectTagIdsByPromptHistoryId(@Param("promptHistoryId") Long promptHistoryId);

    /**
     * 해당 태그가 프롬프트 히스토리에서 사용 중인지 여부 조회 (삭제 방어용)
     */
    boolean existsByTagId(@Param("tagId") Long tagId);
}

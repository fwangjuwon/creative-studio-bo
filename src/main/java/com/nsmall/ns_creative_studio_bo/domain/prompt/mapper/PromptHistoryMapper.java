package com.nsmall.ns_creative_studio_bo.domain.prompt.mapper;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * PromptHistory MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Mapper
public interface PromptHistoryMapper {

    void insert(PromptHistory promptHistory);

    Optional<PromptHistory> selectById(Long id);

    /**
     * 프롬프트 히스토리 목록 조회
     *
     * <pre>
     * teamName != null → 해당 팀 소속 유저들의 히스토리만 조회 (USER 권한)
     * teamName == null → 전체 히스토리 조회 (MANAGER / ADMIN 권한)
     * </pre>
     */
    List<PromptHistory> selectList(@Param("teamName") String teamName);

    /**
     * 오늘 날짜 기준 해당 유저의 프롬프트 생성 횟수 조회 (일일 한도 체크용)
     */
    int countTodayByUserId(@Param("userId") Long userId);
}

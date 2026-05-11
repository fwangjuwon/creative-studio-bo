package com.nsmall.ns_creative_studio_bo.domain.auth.mapper;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistEmpMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistEmpMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * NsistEmpMapping MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Mapper
public interface NsistEmpMappingMapper {

    void insert(NsistEmpMapping mapping);

    Optional<NsistEmpMapping> selectByEmpCd(@Param("empCd") String empCd);

    List<NsistEmpMappingRespDto> selectList();

    void deleteByEmpCd(@Param("empCd") String empCd);
}

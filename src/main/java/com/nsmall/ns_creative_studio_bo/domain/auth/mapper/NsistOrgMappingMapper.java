package com.nsmall.ns_creative_studio_bo.domain.auth.mapper;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistOrgMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistOrgMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * NsistOrgMapping MyBatis Mapper 인터페이스
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Mapper
public interface NsistOrgMappingMapper {

    void insert(NsistOrgMapping mapping);

    Optional<NsistOrgMapping> selectByOrgCd(@Param("orgCd") String orgCd);

    Optional<NsistOrgMappingRespDto> selectByOrgCdWithUser(@Param("orgCd") String orgCd);

    List<NsistOrgMappingRespDto> selectList();

    void deleteByOrgCd(@Param("orgCd") String orgCd);
}

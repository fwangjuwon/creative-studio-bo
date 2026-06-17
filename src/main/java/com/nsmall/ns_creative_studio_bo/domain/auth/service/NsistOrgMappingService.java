package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistOrgMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistOrgMappingRespDto;

import java.util.List;

/**
 * NsistOrgMapping 서비스 인터페이스 — nsist 조직코드↔NCS 팀계정 매핑 관리 (ADMIN 전용)
 *
 * @author gaeddo
 * @since 2026-06-17
 */
public interface NsistOrgMappingService {

    NsistOrgMappingRespDto createMapping(NsistOrgMappingCreateReqDto dto);

    List<NsistOrgMappingRespDto> getMappings();

    void deleteMapping(String orgCd);
}

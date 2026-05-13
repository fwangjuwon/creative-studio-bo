package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistEmpMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistEmpMappingRespDto;

import java.util.List;

/**
 * NsistEmpMapping 서비스 인터페이스 — nsist 사원번호↔NCS 계정 매핑 관리 (ADMIN 전용)
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
public interface NsistEmpMappingService {

    NsistEmpMappingRespDto createMapping(NsistEmpMappingCreateReqDto dto);

    List<NsistEmpMappingRespDto> getMappings();

    void deleteMapping(String empCd);
}

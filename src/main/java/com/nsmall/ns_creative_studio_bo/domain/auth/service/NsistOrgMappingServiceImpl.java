package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.auth.NsistOrgMappingErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistOrgMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistOrgMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistOrgMapping;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistOrgMappingMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NsistOrgMapping 서비스 구현체 — nsist 조직코드↔NCS 팀계정 매핑 관리 (ADMIN 전용)
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NsistOrgMappingServiceImpl implements NsistOrgMappingService {

    private final NsistOrgMappingMapper nsistOrgMappingMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public NsistOrgMappingRespDto createMapping(NsistOrgMappingCreateReqDto dto) {
        nsistOrgMappingMapper.selectByOrgCd(dto.getOrgCd()).ifPresent(m -> {
            throw new BizException(NsistOrgMappingErrorCode.DUPLICATE_ORG_CD);
        });

        userMapper.selectById(dto.getUserId()).orElseThrow(() ->
                new BizException(NsistOrgMappingErrorCode.USER_NOT_FOUND_FOR_MAPPING));

        String requesterId = SecurityUtil.getCurrentLoginId();
        LocalDateTime now = LocalDateTime.now();

        NsistOrgMapping mapping = NsistOrgMapping.builder()
                .orgCd(dto.getOrgCd())
                .userId(dto.getUserId())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        nsistOrgMappingMapper.insert(mapping);

        return nsistOrgMappingMapper.selectByOrgCdWithUser(dto.getOrgCd())
                .orElseThrow(() -> new BizException(NsistOrgMappingErrorCode.MAPPING_NOT_FOUND));
    }

    @Override
    public List<NsistOrgMappingRespDto> getMappings() {
        return nsistOrgMappingMapper.selectList();
    }

    @Override
    @Transactional
    public void deleteMapping(String orgCd) {
        nsistOrgMappingMapper.selectByOrgCd(orgCd).orElseThrow(() ->
                new BizException(NsistOrgMappingErrorCode.MAPPING_NOT_FOUND));

        nsistOrgMappingMapper.deleteByOrgCd(orgCd);
    }
}

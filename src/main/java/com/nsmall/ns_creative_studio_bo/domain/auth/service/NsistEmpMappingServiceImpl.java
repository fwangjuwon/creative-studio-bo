package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.auth.NsistEmpMappingErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistEmpMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistEmpMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.entity.NsistEmpMapping;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistEmpMappingMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NsistEmpMapping 서비스 구현체 — nsist 사원번호↔NCS 계정 매핑 관리 (ADMIN 전용)
 *
 * @author jjhoo8936
 * @since 2026-05-11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NsistEmpMappingServiceImpl implements NsistEmpMappingService {

    private final NsistEmpMappingMapper nsistEmpMappingMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public NsistEmpMappingRespDto createMapping(NsistEmpMappingCreateReqDto dto) {
        nsistEmpMappingMapper.selectByEmpCd(dto.getEmpCd()).ifPresent(m -> {
            throw new BizException(NsistEmpMappingErrorCode.DUPLICATE_EMP_CD);
        });

        userMapper.selectById(dto.getUserId()).orElseThrow(() ->
                new BizException(NsistEmpMappingErrorCode.USER_NOT_FOUND_FOR_MAPPING));

        String requesterId = SecurityUtil.getCurrentLoginId();
        LocalDateTime now = LocalDateTime.now();

        NsistEmpMapping mapping = NsistEmpMapping.builder()
                .empCd(dto.getEmpCd())
                .userId(dto.getUserId())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        nsistEmpMappingMapper.insert(mapping);

        return nsistEmpMappingMapper.selectList().stream()
                .filter(r -> r.getEmpCd().equals(dto.getEmpCd()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<NsistEmpMappingRespDto> getMappings() {
        return nsistEmpMappingMapper.selectList();
    }

    @Override
    @Transactional
    public void deleteMapping(String empCd) {
        nsistEmpMappingMapper.selectByEmpCd(empCd).orElseThrow(() ->
                new BizException(NsistEmpMappingErrorCode.MAPPING_NOT_FOUND));

        nsistEmpMappingMapper.deleteByEmpCd(empCd);
    }
}

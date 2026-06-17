package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.auth.NsistOrgMappingErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistOrgMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistOrgMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistOrgMappingMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NsistOrgMappingService 통합 테스트 — nsist 조직코드↔NCS 팀계정 매핑 관리 비즈니스 로직 검증
 *
 * @author gaeddo
 * @since 2026-06-17
 */
@SpringBootTest
@ActiveProfiles("local")
public class NsistOrgMappingServiceTest {

    @Autowired
    private NsistOrgMappingService nsistOrgMappingService;

    @Autowired
    private NsistOrgMappingMapper nsistOrgMappingMapper;

    @Autowired
    private UserService userService;

    // ================================================================
    // 테스트 데이터 헬퍼
    // ================================================================

    private UserRespDto 테스트_유저_생성(String loginIdSuffix) {
        return userService.createUser(UserCreateReqDto.builder()
                .loginId("org_t_" + loginIdSuffix)
                .password("TestPass1!")
                .name("org테스트_" + loginIdSuffix)
                .role("USER")
                .teamName("테스트팀")
                .dailyLimit(10)
                .build());
    }

    private NsistOrgMappingCreateReqDto 매핑_생성_DTO(String orgCd, Long userId) {
        return NsistOrgMappingCreateReqDto.builder()
                .orgCd(orgCd)
                .userId(userId)
                .build();
    }

    // ================================================================
    // 매핑 등록
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 매핑 등록 - orgCd/userId/loginId 검증")
    void 성공_매핑_등록_정상() {
        UserRespDto user = 테스트_유저_생성("reg1");

        NsistOrgMappingRespDto result = nsistOrgMappingService.createMapping(
                매핑_생성_DTO("O000000001", user.getId()));

        assertThat(result).isNotNull();
        assertThat(result.getOrgCd()).isEqualTo("O000000001");
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getLoginId()).isEqualTo(user.getLoginId());
        assertThat(result.getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 매핑 등록 - 중복 orgCd")
    void 예외_매핑_등록_중복_orgCd() {
        UserRespDto user = 테스트_유저_생성("dup1");

        nsistOrgMappingService.createMapping(매핑_생성_DTO("O000000002", user.getId()));

        assertThatThrownBy(() -> nsistOrgMappingService.createMapping(
                매핑_생성_DTO("O000000002", user.getId())))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistOrgMappingErrorCode.DUPLICATE_ORG_CD));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 매핑 등록 - 존재하지 않는 userId")
    void 예외_매핑_등록_존재하지않는_userId() {
        assertThatThrownBy(() -> nsistOrgMappingService.createMapping(
                매핑_생성_DTO("O000000003", Long.MAX_VALUE)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistOrgMappingErrorCode.USER_NOT_FOUND_FOR_MAPPING));
    }

    // ================================================================
    // 매핑 목록 조회
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 매핑 목록 조회 - 등록 후 목록에 포함 및 필드 검증")
    void 성공_매핑_목록_조회() {
        UserRespDto user = 테스트_유저_생성("list1");
        nsistOrgMappingService.createMapping(매핑_생성_DTO("O000000004", user.getId()));

        List<NsistOrgMappingRespDto> result = nsistOrgMappingService.getMappings();

        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(m -> {
            assertThat(m.getOrgCd()).isNotBlank();
            assertThat(m.getUserId()).isNotNull();
            assertThat(m.getLoginId()).isNotBlank();
            assertThat(m.getName()).isNotBlank();
        });
    }

    // ================================================================
    // 매핑 삭제
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 매핑 삭제 - 삭제 후 DB에서 미존재 확인")
    void 성공_매핑_삭제() {
        UserRespDto user = 테스트_유저_생성("del1");
        nsistOrgMappingService.createMapping(매핑_생성_DTO("O000000005", user.getId()));

        nsistOrgMappingService.deleteMapping("O000000005");

        assertThat(nsistOrgMappingMapper.selectByOrgCd("O000000005")).isEmpty();
    }

    @Test
    @DisplayName("[예외] 매핑 삭제 - 존재하지 않는 orgCd")
    void 예외_매핑_삭제_존재하지않는_orgCd() {
        assertThatThrownBy(() -> nsistOrgMappingService.deleteMapping("ZZZZZZZZZ"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistOrgMappingErrorCode.MAPPING_NOT_FOUND));
    }
}

package com.nsmall.ns_creative_studio_bo.domain.auth.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.auth.NsistEmpMappingErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.request.NsistEmpMappingCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.dto.response.NsistEmpMappingRespDto;
import com.nsmall.ns_creative_studio_bo.domain.auth.mapper.NsistEmpMappingMapper;
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
 * NsistEmpMappingService 통합 테스트 — nsist 사원번호↔NCS 계정 매핑 관리 비즈니스 로직 검증
 *
 * @author jjhoo8936
 * @since 2026-05-13
 */
@SpringBootTest
@ActiveProfiles("local")
public class NsistEmpMappingServiceTest {

    @Autowired
    private NsistEmpMappingService nsistEmpMappingService;

    @Autowired
    private NsistEmpMappingMapper nsistEmpMappingMapper;

    @Autowired
    private UserService userService;

    // ================================================================
    // 테스트 데이터 헬퍼
    // ================================================================

    private UserRespDto 테스트_유저_생성(String loginIdSuffix) {
        return userService.createUser(UserCreateReqDto.builder()
                .loginId("nsist_t_" + loginIdSuffix)
                .password("TestPass1!")
                .name("nsist테스트_" + loginIdSuffix)
                .role("USER")
                .teamName("테스트팀")
                .dailyLimit(10)
                .build());
    }

    private NsistEmpMappingCreateReqDto 매핑_생성_DTO(String empCd, Long userId) {
        return NsistEmpMappingCreateReqDto.builder()
                .empCd(empCd)
                .userId(userId)
                .build();
    }

    // ================================================================
    // 매핑 등록
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 매핑 등록 - empCd/userId/loginId 검증")
    void 성공_매핑_등록_정상() {
        UserRespDto user = 테스트_유저_생성("reg1");

        NsistEmpMappingRespDto result = nsistEmpMappingService.createMapping(
                매핑_생성_DTO("T0001", user.getId()));

        assertThat(result).isNotNull();
        assertThat(result.getEmpCd()).isEqualTo("T0001");
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getLoginId()).isEqualTo(user.getLoginId());
        assertThat(result.getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 매핑 등록 - 여러 empCd가 하나의 userId에 매핑 가능 (N:1)")
    void 성공_매핑_등록_여러_empCd_동일_userId() {
        UserRespDto user = 테스트_유저_생성("reg2");

        nsistEmpMappingService.createMapping(매핑_생성_DTO("T0002", user.getId()));
        nsistEmpMappingService.createMapping(매핑_생성_DTO("T0003", user.getId()));

        List<NsistEmpMappingRespDto> result = nsistEmpMappingService.getMappings();
        long count = result.stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .count();

        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 매핑 등록 - 중복 empCd")
    void 예외_매핑_등록_중복_empCd() {
        UserRespDto user = 테스트_유저_생성("dup1");

        nsistEmpMappingService.createMapping(매핑_생성_DTO("T0004", user.getId()));

        assertThatThrownBy(() -> nsistEmpMappingService.createMapping(
                매핑_생성_DTO("T0004", user.getId())))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistEmpMappingErrorCode.DUPLICATE_EMP_CD));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 매핑 등록 - 존재하지 않는 userId")
    void 예외_매핑_등록_존재하지않는_userId() {
        assertThatThrownBy(() -> nsistEmpMappingService.createMapping(
                매핑_생성_DTO("T0005", Long.MAX_VALUE)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistEmpMappingErrorCode.USER_NOT_FOUND_FOR_MAPPING));
    }

    // ================================================================
    // 매핑 목록 조회
    // ================================================================

    @Test
    @DisplayName("[성공] 매핑 목록 조회 - 시드 데이터 포함 여부 및 필드 검증")
    void 성공_매핑_목록_조회() {
        List<NsistEmpMappingRespDto> result = nsistEmpMappingService.getMappings();

        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(m -> {
            assertThat(m.getEmpCd()).isNotBlank();
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
        nsistEmpMappingService.createMapping(매핑_생성_DTO("T0007", user.getId()));

        nsistEmpMappingService.deleteMapping("T0007");

        assertThat(nsistEmpMappingMapper.selectByEmpCd("T0007")).isEmpty();
    }

    @Test
    @DisplayName("[예외] 매핑 삭제 - 존재하지 않는 empCd")
    void 예외_매핑_삭제_존재하지않는_empCd() {
        assertThatThrownBy(() -> nsistEmpMappingService.deleteMapping("ZZZZZ"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(NsistEmpMappingErrorCode.MAPPING_NOT_FOUND));
    }
}

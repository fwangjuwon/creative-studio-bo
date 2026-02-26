package com.nsmall.ns_creative_studio_bo.domain.user.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.user.UserErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@SpringBootTest
@ActiveProfiles("local")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String TEST_ID_PREFIX = "test_svc_";

    private UserCreateReqDto 기본_생성_DTO(String loginId) {
        return UserCreateReqDto.builder()
                .loginId(loginId)
                .password("testPassword123!")
                .name("테스트유저")
                .role("USER")
                .teamName("개발팀")
                .dailyLimit(10)
                .build();
    }

    // ============================================================
    // 회원 생성
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 생성 - 비밀번호 BCrypt 암호화 검증")
    void 성공_회원_생성_비밀번호_BCrypt_암호화() {
        String rawPassword = "testPassword123!";
        UserCreateReqDto dto = UserCreateReqDto.builder()
                .loginId(TEST_ID_PREFIX + "bcrypt")
                .password(rawPassword)
                .name("암호화테스트유저")
                .role("USER")
                .teamName("개발팀")
                .dailyLimit(5)
                .build();

        userService.createUser(dto);

        User savedUser = userMapper.selectByLoginId(TEST_ID_PREFIX + "bcrypt").orElseThrow();

        assertThat(savedUser.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 회원 생성 - 중복 loginId")
    void 예외_회원_생성_중복_loginId() {
        String loginId = TEST_ID_PREFIX + "dup";
        userService.createUser(기본_생성_DTO(loginId));

        assertThatThrownBy(() -> userService.createUser(기본_생성_DTO(loginId)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.DUPLICATE_LOGIN_ID));
    }

    // ============================================================
    // 회원 단건 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 단건 조회 - 존재하는 회원")
    void 성공_회원_단건_조회_존재() {
        UserRespDto created = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "get"));

        Optional<UserRespDto> result = userService.getUser(created.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getLoginId()).isEqualTo(TEST_ID_PREFIX + "get");
        assertThat(result.get().getName()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("[없음] 회원 단건 조회 - 미존재 회원")
    void 없음_회원_단건_조회_미존재() {
        Optional<UserRespDto> result = userService.getUser(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // ============================================================
    // 회원 목록 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 목록 조회 - 전체")
    void 성공_회원_목록_조회_전체() {
        userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "list1"));
        userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "list2"));

        List<UserRespDto> result = userService.getUsers(UserSearchReqDto.builder().build());

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
            "검색팀A, USER,    검색유저A, search_teamA",
            "검색팀B, MANAGER, 검색유저B, search_teamB"
    })
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 목록 조회 - 조건별 필터링 (팀명 / 권한)")
    void 성공_회원_목록_조회_조건별_필터링(String teamName, String role, String name, String loginIdSuffix) {
        UserCreateReqDto dto = UserCreateReqDto.builder()
                .loginId(TEST_ID_PREFIX + loginIdSuffix)
                .password("testPassword123!")
                .name(name)
                .role(role)
                .teamName(teamName)
                .dailyLimit(10)
                .build();
        userService.createUser(dto);

        List<UserRespDto> byTeam = userService.getUsers(
                UserSearchReqDto.builder().teamName(teamName).build());
        assertThat(byTeam).isNotEmpty();
        assertThat(byTeam).allSatisfy(u -> assertThat(u.getTeamName()).isEqualTo(teamName));

        List<UserRespDto> byRole = userService.getUsers(
                UserSearchReqDto.builder().role(role).build());
        assertThat(byRole).isNotEmpty();
        assertThat(byRole).allSatisfy(u -> assertThat(u.getRole()).isEqualTo(role));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 목록 조회 - 이름 검색 (LIKE)")
    void 성공_회원_목록_조회_이름_검색() {
        userService.createUser(UserCreateReqDto.builder()
                .loginId(TEST_ID_PREFIX + "name_search")
                .password("testPassword123!")
                .name("특정이름유저")
                .role("USER")
                .teamName("검색팀")
                .dailyLimit(10)
                .build());

        List<UserRespDto> result = userService.getUsers(
                UserSearchReqDto.builder().name("특정이름").build());

        assertThat(result).isNotEmpty();
        assertThat(result).anySatisfy(u -> assertThat(u.getName()).contains("특정이름"));
    }

    // ============================================================
    // 회원 수정
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 수정 - 이름/권한/팀명/한도 변경")
    void 성공_회원_수정() {
        UserRespDto created = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "update"));

        UserUpdateReqDto updateDto = UserUpdateReqDto.builder()
                .name("수정된이름")
                .role("MANAGER")
                .teamName("수정팀")
                .dailyLimit(20)
                .build();

        UserRespDto updated = userService.updateUser(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정된이름");
        assertThat(updated.getRole()).isEqualTo("MANAGER");
        assertThat(updated.getTeamName()).isEqualTo("수정팀");
        assertThat(updated.getDailyLimit()).isEqualTo(20);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 수정 - 비밀번호 변경 후 BCrypt 검증")
    void 성공_회원_수정_비밀번호_변경() {
        UserRespDto created = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "update_pw"));
        String newPassword = "newPassword456!";

        userService.updateUser(created.getId(),
                UserUpdateReqDto.builder().password(newPassword).build());

        User user = userMapper.selectById(created.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("[예외] 회원 수정 - 미존재 회원")
    void 예외_회원_수정_미존재() {
        UserUpdateReqDto updateDto = UserUpdateReqDto.builder().name("수정요청").build();

        assertThatThrownBy(() -> userService.updateUser(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    // ============================================================
    // 회원 삭제
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 회원 삭제 - 삭제 후 조회 시 미존재")
    void 성공_회원_삭제() {
        UserRespDto created = userService.createUser(기본_생성_DTO(TEST_ID_PREFIX + "delete"));

        userService.deleteUser(created.getId());

        Optional<UserRespDto> result = userService.getUser(created.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[예외] 회원 삭제 - 미존재 회원")
    void 예외_회원_삭제_미존재() {
        assertThatThrownBy(() -> userService.deleteUser(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }
}

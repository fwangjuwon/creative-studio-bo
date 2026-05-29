package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * PromptHelper 단위 테스트
 *
 * <pre>
 * UserMapper, PromptHistoryMapper는 Mockito로 모킹하고,
 * ObjectMapper는 실제 인스턴스를 사용하여 직렬화/역직렬화 로직을 검증한다.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-28
 */
@ExtendWith(MockitoExtension.class)
public class PromptHelperTest {

    @InjectMocks
    private PromptHelper promptHelper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PromptHistoryMapper promptHistoryMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final String IMAGE_BASE_URL = "https://test.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(promptHelper, "imageBaseUrl", IMAGE_BASE_URL);
        ReflectionTestUtils.setField(promptHelper, "objectMapper", new ObjectMapper());
    }

    // ================================================================
    // checkDailyLimit
    // ================================================================

    @Test
    @DisplayName("[성공] checkDailyLimit - 한도 미초과")
    void 성공_checkDailyLimit_한도_미초과() {
        User user = User.builder().id(TEST_USER_ID).dailyLimit(10).build();
        given(userMapper.selectById(TEST_USER_ID)).willReturn(Optional.of(user));
        given(promptHistoryMapper.countTodayByUserId(TEST_USER_ID)).willReturn(5);

        // 예외 없이 통과
        promptHelper.checkDailyLimit(TEST_USER_ID);
    }

    @Test
    @DisplayName("[예외] checkDailyLimit - 사용자 없음")
    void 예외_checkDailyLimit_사용자_없음() {
        given(userMapper.selectById(TEST_USER_ID)).willReturn(Optional.empty());

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> promptHelper.checkDailyLimit(TEST_USER_ID))
                .extracting(BizException::getErrorCode)
                .isEqualTo(PromptErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[예외] checkDailyLimit - 일일 한도 초과")
    void 예외_checkDailyLimit_일일_한도_초과() {
        User user = User.builder().id(TEST_USER_ID).dailyLimit(10).build();
        given(userMapper.selectById(TEST_USER_ID)).willReturn(Optional.of(user));
        given(promptHistoryMapper.countTodayByUserId(TEST_USER_ID)).willReturn(10);

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> promptHelper.checkDailyLimit(TEST_USER_ID))
                .extracting(BizException::getErrorCode)
                .isEqualTo(PromptErrorCode.DAILY_LIMIT_EXCEEDED);
    }

    // ================================================================
    // toAbsoluteUrls
    // ================================================================

    @Test
    @DisplayName("[성공] toAbsoluteUrls - 상대 경로 → 절대 URL 변환")
    void 성공_toAbsoluteUrls_정상_변환() {
        List<String> result = promptHelper.toAbsoluteUrls(
                List.of("/images/a.jpg", "/images/b.jpg"));

        assertThat(result).containsExactly(
                IMAGE_BASE_URL + "/images/a.jpg",
                IMAGE_BASE_URL + "/images/b.jpg");
    }

    @Test
    @DisplayName("[성공] toAbsoluteUrls - 빈 리스트")
    void 성공_toAbsoluteUrls_빈_리스트() {
        assertThat(promptHelper.toAbsoluteUrls(List.of())).isEmpty();
    }

    // ================================================================
    // serializeImageUrls
    // ================================================================

    @Test
    @DisplayName("[성공] serializeImageUrls - 정상 직렬화")
    void 성공_serializeImageUrls_정상() {
        String result = promptHelper.serializeImageUrls(
                List.of("/images/a.jpg", "/images/b.jpg"));

        assertThat(result).isEqualTo("[\"/images/a.jpg\",\"/images/b.jpg\"]");
    }

    @Test
    @DisplayName("[성공] serializeImageUrls - 빈 리스트 → '[]' 반환")
    void 성공_serializeImageUrls_빈_리스트() {
        assertThat(promptHelper.serializeImageUrls(List.of())).isEqualTo("[]");
    }

    @Test
    @DisplayName("[성공] serializeImageUrls - null → '[]' 반환")
    void 성공_serializeImageUrls_null() {
        assertThat(promptHelper.serializeImageUrls(null)).isEqualTo("[]");
    }

    // ================================================================
    // parseImageUrls
    // ================================================================

    @Test
    @DisplayName("[성공] parseImageUrls - JSON 배열 파싱")
    void 성공_parseImageUrls_JSON_배열() {
        List<String> result = promptHelper.parseImageUrls(
                "[\"/images/a.jpg\",\"/images/b.jpg\"]");

        assertThat(result).containsExactly("/images/a.jpg", "/images/b.jpg");
    }

    @Test
    @DisplayName("[성공] parseImageUrls - 단일 문자열")
    void 성공_parseImageUrls_단일_문자열() {
        List<String> result = promptHelper.parseImageUrls("/images/a.jpg");

        assertThat(result).containsExactly("/images/a.jpg");
    }

    @Test
    @DisplayName("[성공] parseImageUrls - null/blank → 빈 리스트 반환")
    void 성공_parseImageUrls_null_또는_blank() {
        assertThat(promptHelper.parseImageUrls(null)).isEmpty();
        assertThat(promptHelper.parseImageUrls("  ")).isEmpty();
    }

    @Test
    @DisplayName("[성공] parseImageUrls - JSON 파싱 실패 → 원본 단일 원소 목록 반환")
    void 성공_parseImageUrls_파싱_실패_폴백() {
        List<String> result = promptHelper.parseImageUrls("[invalid-json");

        assertThat(result).containsExactly("[invalid-json");
    }
}

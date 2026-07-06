package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptConstants;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptType;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FusionImageService 통합 테스트 — 이미지 유효성 검증 + 합성 이미지 생성 흐름 검증
 *
 * <pre>
 * ImageGenerationService, UserMapper, PromptHistoryMapper는 @MockitoBean으로 모킹하여
 * 외부 API 및 DB 접근 없이 테스트한다.
 * UserMapper, PromptHistoryMapper는 PromptHelper를 통해 간접 주입된다.
 * 이미지 유효성 검증은 ImageValidationUtil 위임 — 별도 단위 테스트는 ImageValidationUtilTest 참고.
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-19
 */
@SpringBootTest
@ActiveProfiles("local")
public class FusionImageServiceTest {

    @Autowired
    private FusionImageService fusionImageService;

    @MockitoBean
    private ImageGenerationService imageGenerationService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private PromptHistoryMapper promptHistoryMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final byte[] DUMMY_BYTES = new byte[]{1, 2, 3};

    @BeforeEach
    void setUp() {
        User mockUser = User.builder().id(TEST_USER_ID).dailyLimit(10).build();
        given(userMapper.selectById(TEST_USER_ID)).willReturn(Optional.of(mockUser));
        given(promptHistoryMapper.countTodayByUserId(anyLong())).willReturn(0);
    }

    private MockMultipartFile 유효한_이미지(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", DUMMY_BYTES);
    }

    private MockMultipartFile 크기_초과_이미지(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[16 * 1024 * 1024]);
    }

    private MockMultipartFile 미허용_MIME_이미지(String name, String mimeType) {
        return new MockMultipartFile(name, name + ".file", mimeType, DUMMY_BYTES);
    }

    // ================================================================
    // 합성 이미지 생성 — 성공
    // ================================================================

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - 정상 케이스")
    void 성공_합성_이미지_생성_정상() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt(), any()))
                .willReturn(List.of("/images/fusion-1.jpg", "/images/fusion-2.jpg", "/images/fusion-3.jpg"));

        List<String> result = fusionImageService.generateFusionImages(
                TEST_USER_ID,
                유효한_이미지("productImage"),
                유효한_이미지("personImage"),
                "상품을 양손으로 들고 있는", "밝은 스튜디오 배경", "1:1", null, null
        );

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(url -> url.startsWith("http"));
    }

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - pose/instruction/imageAspectRatio 모두 null")
    void 성공_합성_이미지_생성_선택_파라미터_null() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt(), any()))
                .willReturn(List.of("/images/fusion-1.jpg"));

        List<String> result = fusionImageService.generateFusionImages(
                TEST_USER_ID, 유효한_이미지("productImage"), 유효한_이미지("personImage"), null, null, null, null, null);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("[성공] 합성 이미지 생성 - userMessage에 pose/instruction/aspectRatio 포함 검증")
    void 성공_합성_이미지_생성_userMessage_구성_검증() {
        given(imageGenerationService.generateFusionImages(any(), any(), any(), any(), any(), anyInt(), any()))
                .willReturn(List.of("/images/fusion-1.jpg"));

        fusionImageService.generateFusionImages(
                TEST_USER_ID, 유효한_이미지("productImage"), 유효한_이미지("personImage"),
                "상품을 들고 있는", "따뜻한 배경", "16:9", null, null);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageGenerationService).generateFusionImages(
                messageCaptor.capture(), any(), any(), any(), any(), eq(PromptConstants.IMAGE_GENERATE_COUNT), eq("16:9"));

        String captured = messageCaptor.getValue();
        assertThat(captured).contains("상품을 들고 있는");
        assertThat(captured).contains("따뜻한 배경");
        assertThat(captured).contains("16:9");
    }

    // ================================================================
    // 합성 이미지 생성 — 예외
    // ================================================================

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 상품 이미지 크기 초과")
    void 예외_합성_이미지_생성_상품_크기_초과() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                TEST_USER_ID, 크기_초과_이미지("productImage"), 유효한_이미지("personImage"), null, null, null, null, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 인물 이미지 크기 초과")
    void 예외_합성_이미지_생성_인물_크기_초과() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                TEST_USER_ID, 유효한_이미지("productImage"), 크기_초과_이미지("personImage"), null, null, null, null, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 지원하지 않는 MIME 타입")
    void 예외_합성_이미지_생성_미허용_MIME() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                TEST_USER_ID, 미허용_MIME_이미지("productImage", "image/gif"), 유효한_이미지("personImage"), null, null, null, null, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 상품 이미지 누락(null)")
    void 예외_합성_이미지_생성_상품_이미지_누락() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                TEST_USER_ID, null, 유효한_이미지("personImage"), null, null, null, null, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 합성 이미지 생성 - 인물 이미지 누락(null)")
    void 예외_합성_이미지_생성_인물_이미지_누락() {
        assertThatThrownBy(() -> fusionImageService.generateFusionImages(
                TEST_USER_ID, 유효한_이미지("productImage"), null, null, null, null, null, null))
                .isInstanceOf(BizException.class);
    }

    // ================================================================
    // 텍스트 합성 이미지 생성 — 성공
    // ================================================================

    @Test
    @DisplayName("[성공] 텍스트 합성 이미지 생성 - 메인 텍스트만")
    void 성공_텍스트_합성_이미지_생성_메인_텍스트만() {
        given(imageGenerationService.generateTextOverlayImages(anyString(), any(), anyString(), anyInt()))
                .willReturn(List.of("/images/text-1.jpg", "/images/text-2.jpg", "/images/text-3.jpg"));

        List<String> result = fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), "LAMP COOK", null);

        assertThat(result).hasSize(PromptConstants.IMAGE_GENERATE_COUNT);
        assertThat(result).allMatch(url -> url.startsWith("http"));
    }

    @Test
    @DisplayName("[성공] 텍스트 합성 이미지 생성 - 단서 문구만")
    void 성공_텍스트_합성_이미지_생성_단서_문구만() {
        given(imageGenerationService.generateTextOverlayImages(anyString(), any(), anyString(), anyInt()))
                .willReturn(List.of("/images/text-1.jpg", "/images/text-2.jpg", "/images/text-3.jpg"));

        List<String> result = fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), null, "주의사항을 꼭 읽어주세요");

        assertThat(result).hasSize(PromptConstants.IMAGE_GENERATE_COUNT);
        assertThat(result).allMatch(url -> url.startsWith("http"));
    }

    @Test
    @DisplayName("[성공] 텍스트 합성 이미지 생성 - 메인+단서 동시")
    void 성공_텍스트_합성_이미지_생성_메인_단서_동시() {
        given(imageGenerationService.generateTextOverlayImages(anyString(), any(), anyString(), anyInt()))
                .willReturn(List.of("/images/text-1.jpg", "/images/text-2.jpg", "/images/text-3.jpg"));

        List<String> result = fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), "LAMP COOK", "주의사항을 꼭 읽어주세요");

        assertThat(result).hasSize(PromptConstants.IMAGE_GENERATE_COUNT);
    }

    @Test
    @DisplayName("[성공] 텍스트 합성 이미지 생성 - PromptHistory TEXT_OVL 타입으로 저장 검증")
    void 성공_텍스트_합성_이미지_생성_히스토리_저장_검증() {
        given(imageGenerationService.generateTextOverlayImages(anyString(), any(), anyString(), anyInt()))
                .willReturn(List.of("/images/text-1.jpg", "/images/text-2.jpg", "/images/text-3.jpg"));

        fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), "메인 텍스트", "단서 문구");

        ArgumentCaptor<PromptHistory> historyCaptor = ArgumentCaptor.forClass(PromptHistory.class);
        verify(promptHistoryMapper).insert(historyCaptor.capture());

        PromptHistory saved = historyCaptor.getValue();
        assertThat(saved.getPromptType()).isEqualTo(PromptType.TEXT_OVL.name());
        assertThat(saved.getTextContent()).isEqualTo("메인 텍스트");
        assertThat(saved.getDisclaimerText()).isEqualTo("단서 문구");
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
    }

    // ================================================================
    // 텍스트 합성 이미지 생성 — 예외
    // ================================================================

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 이미지 누락(null)")
    void 예외_텍스트_합성_이미지_생성_이미지_누락() {
        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, null, "텍스트", null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 이미지 크기 초과")
    void 예외_텍스트_합성_이미지_생성_이미지_크기_초과() {
        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 크기_초과_이미지("image"), "텍스트", null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 지원하지 않는 MIME 타입")
    void 예외_텍스트_합성_이미지_생성_미허용_MIME() {
        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 미허용_MIME_이미지("image", "image/gif"), "텍스트", null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 텍스트 없음 (둘 다 null)")
    void 예외_텍스트_합성_이미지_생성_텍스트_없음() {
        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), null, null))
                .isInstanceOf(BizException.class);
        verify(promptHistoryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 텍스트 없음 (둘 다 공백)")
    void 예외_텍스트_합성_이미지_생성_텍스트_공백() {
        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), "   ", "   "))
                .isInstanceOf(BizException.class);
        verify(promptHistoryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("[예외] 텍스트 합성 이미지 생성 - 일일 한도 초과")
    void 예외_텍스트_합성_이미지_생성_일일_한도_초과() {
        given(promptHistoryMapper.countTodayByUserId(anyLong())).willReturn(10);

        assertThatThrownBy(() -> fusionImageService.generateTextOverlayImages(
                TEST_USER_ID, 유효한_이미지("image"), "텍스트", null))
                .isInstanceOf(BizException.class);
        verify(imageGenerationService, never()).generateTextOverlayImages(anyString(), any(), anyString(), anyInt());
    }
}

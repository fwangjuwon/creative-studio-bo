package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptHistorySearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

/**
 * HumanPromptService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * GeminiService, ImageGenerationService는 외부 API 의존성이므로 @MockitoBean으로 모킹하여 테스트.
 * human_prompt_step / attr / option 시드 데이터는 data.sql에서 로드된다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-09
 */
@SpringBootTest
@ActiveProfiles("local")
public class HumanPromptServiceTest {

    @Autowired
    private HumanPromptService humanPromptService;

    @Autowired
    private PromptService promptService;

    @Autowired
    private PromptHistoryMapper promptHistoryMapper;

    @Autowired
    private UserService userService;

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private ImageGenerationService imageGenerationService;

    private static final String MOCK_PROMPT = "A photorealistic portrait of a young Korean woman, soft natural daylight, blurred background.";

    // ================================================================
    // 테스트 데이터 헬퍼
    // ================================================================

    private UserRespDto 유저_생성(String loginId, String role, String teamName, int dailyLimit) {
        return userService.createUser(UserCreateReqDto.builder()
                .loginId(loginId)
                .password("TestPass1!")
                .name("테스트유저_" + loginId)
                .role(role)
                .teamName(teamName)
                .dailyLimit(dailyLimit)
                .build());
    }

    /**
     * data.sql 시드 데이터에서 is_required=true인 모든 attr에 대해
     * 각 attr의 첫 번째 option ID를 선택하여 selections 맵을 구성한다.
     * HumanPromptServiceImpl의 필수 attr 검증(Step 3)을 통과하기 위한 최소 구성.
     */
    private Map<Long, List<Long>> 최소_선택지_구성() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();
        Map<Long, List<Long>> selections = new java.util.HashMap<>();
        for (HumanPromptStepRespDto step : steps) {
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                if (attr.isRequired()) {
                    Long optionId = attr.getOptions().get(0).getId();
                    selections.put(attr.getId(), List.of(optionId));
                }
            }
        }
        return selections;
    }

    private PromptHistory PERSON_히스토리_직접_삽입(Long userId) {
        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(null)
                .promptType("PERSON")
                .promptStyle("NANO_BANANA")
                .promptText("direct insert person prompt")
                .initRegiPic("test")
                .initRegiDttm(LocalDateTime.now())
                .finalRvisPic("test")
                .finalRvisDttm(LocalDateTime.now())
                .build();
        promptHistoryMapper.insert(history);
        return history;
    }

    // ================================================================
    // 단계 구조 조회
    // ================================================================

    @Test
    @DisplayName("[성공] 단계 구조 조회 - 5단계 반환")
    void 성공_단계_구조_조회_5단계_반환() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();

        assertThat(steps).hasSize(5);
        assertThat(steps.get(0).getStepNumber()).isEqualTo(1);
        assertThat(steps.get(0).getStepCode()).isEqualTo("A");
        assertThat(steps.get(4).getStepNumber()).isEqualTo(5);
        assertThat(steps.get(4).getStepCode()).isEqualTo("E");
    }

    @Test
    @DisplayName("[성공] 단계 구조 조회 - 각 단계에 세부항목과 선택지 포함")
    void 성공_단계_구조_조회_세부항목_선택지_포함() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();

        for (HumanPromptStepRespDto step : steps) {
            assertThat(step.getAttrs()).isNotEmpty();
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                assertThat(attr.getOptions()).isNotEmpty();
            }
        }
    }

    // ================================================================
    // 인물 프롬프트 생성 — 성공
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - 정상 케이스")
    void 성공_인물_프롬프트_생성_정상() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_user_ok", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getPromptType()).isEqualTo("PERSON");
        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - 일일 한도 미만")
    void 성공_인물_프롬프트_생성_일일한도_미만() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_user_limit", "USER", "팀A", 3);
        PERSON_히스토리_직접_삽입(user.getId());
        PERSON_히스토리_직접_삽입(user.getId());

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
    }

    // ================================================================
    // 인물 프롬프트 생성 — 예외
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 인물 프롬프트 생성 - 일일 한도 초과")
    void 예외_인물_프롬프트_생성_일일한도_초과() {
        UserRespDto user = 유저_생성("human_user_over", "USER", "팀B", 2);
        PERSON_히스토리_직접_삽입(user.getId());
        PERSON_히스토리_직접_삽입(user.getId());

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 인물 프롬프트 생성 - 상품 프롬프트 이력도 한도에 합산됨")
    void 예외_인물_프롬프트_생성_상품이력도_한도_합산() {
        UserRespDto user = 유저_생성("human_user_mixed", "USER", "팀C", 2);

        // PRODUCT 이력 2건으로 한도 채우기
        PromptHistory productHistory = PromptHistory.builder()
                .userId(user.getId())
                .categoryId(null)
                .promptType("PRODUCT")
                .promptStyle("NANO_BANANA")
                .promptText("product prompt")
                .initRegiPic("test")
                .initRegiDttm(LocalDateTime.now())
                .finalRvisPic("test")
                .finalRvisDttm(LocalDateTime.now())
                .build();
        promptHistoryMapper.insert(productHistory);
        promptHistoryMapper.insert(productHistory);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("[예외] 인물 프롬프트 생성 - 존재하지 않는 사용자")
    void 예외_인물_프롬프트_생성_사용자_미존재() {
        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        assertThatThrownBy(() -> humanPromptService.createPrompt(Long.MAX_VALUE, dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.USER_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 인물 프롬프트 생성 - 선택 옵션 없음 (빈 selections)")
    void 예외_인물_프롬프트_생성_선택옵션_없음() {
        UserRespDto user = 유저_생성("human_user_noopt", "USER", "팀D", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(Map.of())
                .build();

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.NO_OPTIONS_SELECTED));
    }

    // ================================================================
    // 인물 프롬프트 생성 — 신규 필드 검증
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - optionNames 응답 포함")
    void 성공_인물_프롬프트_생성_optionNames_응답_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("opt_name_user", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result.getOptionNames()).isNotEmpty();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - purpose/additionalNotes 저장 및 응답 포함")
    void 성공_인물_프롬프트_생성_purpose_additionalNotes_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("purpose_user", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .purpose("브랜드 캠페인")
                .additionalNotes("미소 짓는 표정 강조")
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result.getPurpose()).isEqualTo("브랜드 캠페인");
        assertThat(result.getAdditionalNotes()).isEqualTo("미소 짓는 표정 강조");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - promptStyle MID_JOURNEY 명시 저장")
    void 성공_인물_프롬프트_생성_promptStyle_MID_JOURNEY() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_style_mj", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .promptStyle("MID_JOURNEY")
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result.getPromptStyle()).isEqualTo("MID_JOURNEY");
    }

    // ================================================================
    // 이력 조회 — PERSON 신규 필드 검증
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이력 조회 - PERSON 이력에 optionNames/purpose/additionalNotes 포함")
    void 성공_이력_조회_PERSON_신규필드_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("hist_person_detail", "USER", "팀G", 10);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .selections(최소_선택지_구성())
                .purpose("SNS 광고")
                .additionalNotes("밝은 배경")
                .promptStyle("MID_JOURNEY")
                .build();

        humanPromptService.createPrompt(user.getId(), dto);

        PromptHistorySearchReqDto searchReqDto = new PromptHistorySearchReqDto();
        searchReqDto.setPageNum(1);
        searchReqDto.setPageSize(100);
        PageRespDto<PromptHistoryRespDto> page = promptService.getPromptHistories(searchReqDto);
        PromptHistoryRespDto found = page.items().stream()
                .filter(h -> "PERSON".equals(h.getPromptType()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getOptionNames()).isNotEmpty();
        assertThat(found.getPurpose()).isEqualTo("SNS 광고");
        assertThat(found.getAdditionalNotes()).isEqualTo("밝은 배경");
        assertThat(found.getPromptStyle()).isEqualTo("MID_JOURNEY");
    }

    // ================================================================
    // 이미지 생성
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이미지 생성 - 이미지URL과 최종수정자 이력에 업데이트됨")
    void 성공_이미지_생성_이미지URL_최종수정자_업데이트() {
        given(imageGenerationService.generateImages(any(), anyInt()))
                .willReturn(List.of("/images/test-uuid-1.jpg", "/images/test-uuid-2.jpg", "/images/test-uuid-3.jpg"));

        UserRespDto user = 유저_생성("img_gen_user", "USER", "팀E", 5);
        PromptHistory history = PERSON_히스토리_직접_삽입(user.getId());

        List<String> imageUrls = humanPromptService.generateImage(history.getId(), MOCK_PROMPT);

        assertThat(imageUrls).hasSize(3);
        assertThat(imageUrls).contains("http://localhost:8080/images/test-uuid-1.jpg");

        PromptHistory updated = promptHistoryMapper.selectById(history.getId()).orElseThrow();
        assertThat(updated.getImageUrl()).contains("test-uuid-1"); // JSON 배열로 저장됨
        assertThat(updated.getFinalRvisPic()).isEqualTo("system"); // 테스트 환경 SecurityUtil 반환값
        assertThat(updated.getFinalRvisDttm()).isNotNull();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이미지 생성 - 이력ID 없으면 이력 미업데이트")
    void 성공_이미지_생성_이력ID_없으면_이력_미업데이트() {
        given(imageGenerationService.generateImages(any(), anyInt()))
                .willReturn(List.of("/images/test-uuid-1.jpg", "/images/test-uuid-2.jpg", "/images/test-uuid-3.jpg"));

        UserRespDto user = 유저_생성("img_gen_nohistory", "USER", "팀F", 5);
        PromptHistory history = PERSON_히스토리_직접_삽입(user.getId());

        List<String> imageUrls = humanPromptService.generateImage(null, MOCK_PROMPT);

        assertThat(imageUrls).hasSize(3);

        // historyId가 null이므로 이력의 imageUrl은 그대로 null
        PromptHistory unchanged = promptHistoryMapper.selectById(history.getId()).orElseThrow();
        assertThat(unchanged.getImageUrl()).isNull();
        assertThat(unchanged.getFinalRvisPic()).isEqualTo("test"); // 최초 삽입 시 값 그대로
    }

}

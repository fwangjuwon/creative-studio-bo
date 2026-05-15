package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PromptHistorySearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptStyle;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptType;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.PersonSelectionDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import org.mockito.ArgumentCaptor;

/**
 * HumanPromptService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * GeminiService, ImageGenerationService는 외부 API 의존성이므로 @MockitoBean으로 모킹하여 테스트.
 * human_prompt_step / attr / option 시드 데이터는 data.sql에서 로드된다.
 *
 * 단계 구조 (총 3단계):
 *   Step 1 (D, is_common=true):  조명 및 배경
 *   Step 2 (E, is_common=true):  촬영 기술/스타일
 *   Step 3 (A, is_common=false): 핵심 피사체 정의
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
     * 공통 단계(is_common=true)의 필수 attr 최소 선택지 구성.
     */
    private Map<Long, List<Long>> 공통_최소_선택지_구성() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();
        Map<Long, List<Long>> selections = new HashMap<>();
        for (HumanPromptStepRespDto step : steps) {
            if (!step.isCommon()) continue;
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                if (attr.isRequired()) {
                    selections.put(attr.getId(), List.of(attr.getOptions().get(0).getId()));
                }
            }
        }
        return selections;
    }

    /**
     * 개별 단계(is_common=false)의 필수 attr 최소 선택지 구성.
     */
    private Map<Long, List<Long>> 개별_최소_선택지_구성() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();
        Map<Long, List<Long>> selections = new HashMap<>();
        for (HumanPromptStepRespDto step : steps) {
            if (step.isCommon()) continue;
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                if (attr.isRequired()) {
                    selections.put(attr.getId(), List.of(attr.getOptions().get(0).getId()));
                }
            }
        }
        return selections;
    }

    /**
     * 1명 개별 모드 최소 DTO 구성.
     */
    private HumanPromptCreateReqDto 최소_DTO_구성_1명() {
        return HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_최소_선택지_구성())
                        .build()))
                .commonSelections(공통_최소_선택지_구성())
                .build();
    }

    private PromptHistory PERSON_히스토리_직접_삽입(Long userId) {
        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(null)
                .promptType(PromptType.PERSON.name())
                .promptStyle(PromptStyle.NANO_BANANA.name())
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
    @DisplayName("[성공] 단계 구조 조회 - 단계 목록 존재")
    void 성공_단계_구조_조회_단계_존재() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();

        assertThat(steps).isNotEmpty();
        // step_number 오름차순 정렬 확인
        for (int i = 1; i < steps.size(); i++) {
            assertThat(steps.get(i).getStepNumber())
                    .isGreaterThan(steps.get(i - 1).getStepNumber());
        }
    }

    @Test
    @DisplayName("[성공] 단계 구조 조회 - 공통/개별 단계 모두 존재")
    void 성공_단계_구조_조회_isCommon_구분() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();

        assertThat(steps).anyMatch(HumanPromptStepRespDto::isCommon)
                .as("공통 단계(is_common=true)가 최소 1개 존재해야 함");
        assertThat(steps).anyMatch(s -> !s.isCommon())
                .as("개별 단계(is_common=false)가 최소 1개 존재해야 함");
    }

    @Test
    @DisplayName("[성공] 단계 구조 조회 - 각 단계에 세부항목과 선택지 포함")
    void 성공_단계_구조_조회_세부항목_선택지_포함() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();

        assertThat(steps).isNotEmpty();
        for (HumanPromptStepRespDto step : steps) {
            assertThat(step.getAttrs())
                    .as("step %s 에 attr이 존재해야 함", step.getStepCode())
                    .isNotEmpty();
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                assertThat(attr.getOptions())
                        .as("step %s / attr '%s' 에 option이 존재해야 함", step.getStepCode(), attr.getName())
                        .isNotEmpty();
            }
        }
    }

    // ================================================================
    // 인물 프롬프트 생성 — 성공
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - 정상 케이스 (1명 개별 모드)")
    void 성공_인물_프롬프트_생성_정상() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_user_ok", "USER", "팀A", 5);

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), 최소_DTO_구성_1명());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getPromptType()).isEqualTo(PromptType.PERSON.name());
        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - 일일 한도 미만")
    void 성공_인물_프롬프트_생성_일일한도_미만() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_user_limit", "USER", "팀A", 3);
        PERSON_히스토리_직접_삽입(user.getId());
        PERSON_히스토리_직접_삽입(user.getId());

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), 최소_DTO_구성_1명());

        assertThat(result).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - promptStyle MID_JOURNEY 명시 저장")
    void 성공_인물_프롬프트_생성_promptStyle_MID_JOURNEY() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_style_mj", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_최소_선택지_구성())
                        .build()))
                .commonSelections(공통_최소_선택지_구성())
                .promptStyle(PromptStyle.MID_JOURNEY)
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result.getPromptStyle()).isEqualTo(PromptStyle.MID_JOURNEY.name());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - additionalNotes 저장 및 응답 포함")
    void 성공_인물_프롬프트_생성_additionalNotes_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("notes_user", "USER", "팀A", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_최소_선택지_구성())
                        .build()))
                .commonSelections(공통_최소_선택지_구성())
                .additionalNotes("미소 짓는 표정 강조")
                .build();

        PromptHistoryRespDto result = humanPromptService.createPrompt(user.getId(), dto);

        assertThat(result.getAdditionalNotes()).isEqualTo("미소 짓는 표정 강조");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 인물 프롬프트 생성 - 2명 개별 모드 + 관계 입력")
    void 성공_인물_프롬프트_생성_2명_관계_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("human_2persons", "USER", "팀A", 5);
        Map<Long, List<Long>> 개별선택지 = 개별_최소_선택지_구성();

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(
                        PersonSelectionDto.builder().selections(개별선택지).build(),
                        PersonSelectionDto.builder().selections(개별선택지).build()))
                .commonSelections(공통_최소_선택지_구성())
                .relationship("커플")
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

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), 최소_DTO_구성_1명()))
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

        PromptHistory productHistory = PromptHistory.builder()
                .userId(user.getId())
                .categoryId(null)
                .promptType(PromptType.PRODUCT.name())
                .promptStyle(PromptStyle.NANO_BANANA.name())
                .promptText("product prompt")
                .initRegiPic("test")
                .initRegiDttm(LocalDateTime.now())
                .finalRvisPic("test")
                .finalRvisDttm(LocalDateTime.now())
                .build();
        promptHistoryMapper.insert(productHistory);
        promptHistoryMapper.insert(productHistory);

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), 최소_DTO_구성_1명()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("[예외] 인물 프롬프트 생성 - 존재하지 않는 사용자")
    void 예외_인물_프롬프트_생성_사용자_미존재() {
        assertThatThrownBy(() -> humanPromptService.createPrompt(Long.MAX_VALUE, 최소_DTO_구성_1명()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.USER_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 인물 프롬프트 생성 - persons 없음 (빈 목록)")
    void 예외_인물_프롬프트_생성_persons_없음() {
        UserRespDto user = 유저_생성("human_user_noopt", "USER", "팀D", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of())
                .commonSelections(공통_최소_선택지_구성())
                .build();

        assertThatThrownBy(() -> humanPromptService.createPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.NO_OPTIONS_SELECTED));
    }

    // ================================================================
    // 이력 조회 — PERSON 신규 필드 검증
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이력 조회 - PERSON 이력에 personOptions/additionalNotes 포함")
    void 성공_이력_조회_PERSON_신규필드_포함() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("hist_person_detail", "USER", "팀G", 10);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_최소_선택지_구성())
                        .build()))
                .commonSelections(공통_최소_선택지_구성())
                .additionalNotes("밝은 배경")
                .promptStyle(PromptStyle.MID_JOURNEY)
                .build();

        humanPromptService.createPrompt(user.getId(), dto);

        PromptHistorySearchReqDto searchReqDto = new PromptHistorySearchReqDto();
        searchReqDto.setPageNum(1);
        searchReqDto.setPageSize(100);
        PageRespDto<PromptHistoryRespDto> page = promptService.getPromptHistories(searchReqDto);
        PromptHistoryRespDto found = page.items().stream()
                .filter(h -> PromptType.PERSON.name().equals(h.getPromptType()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getPersonOptions()).isNotEmpty();
        assertThat(found.getAdditionalNotes()).isEqualTo("밝은 배경");
        assertThat(found.getPromptStyle()).isEqualTo(PromptStyle.MID_JOURNEY.name());
    }

    // ================================================================
    // attrName 분리 구조 검증 (fix/human-prompt-attr-name-in-prompt)
    // ================================================================

    /**
     * 개별 단계에서 복수 attr(연령대 + 성별 등)를 선택하는 DTO 구성.
     * 각 required attr마다 첫 번째 option을 선택한다.
     */
    private Map<Long, List<Long>> 개별_복수_attr_선택지_구성() {
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();
        Map<Long, List<Long>> selections = new HashMap<>();
        for (HumanPromptStepRespDto step : steps) {
            if (step.isCommon()) continue;
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                // required 여부 상관없이 모든 attr의 첫 번째 option 선택
                if (!attr.getOptions().isEmpty()) {
                    selections.put(attr.getId(), List.of(attr.getOptions().get(0).getId()));
                }
            }
        }
        return selections;
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] attrName 분리 - 1명 복수 attr 선택 시 Gemini 인자가 attrName별로 분리됨")
    @SuppressWarnings("unchecked")
    void 성공_attrName_분리_복수_attr_Gemini_인자_구조_검증() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("attr_split_user", "USER", "팀AttrTest", 5);

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_복수_attr_선택지_구성())
                        .build()))
                .commonSelections(공통_최소_선택지_구성())
                .build();

        humanPromptService.createPrompt(user.getId(), dto);

        // Gemini 호출 시 전달된 personsStepValues 캡처
        ArgumentCaptor<List<Map<String, Map<String, List<String>>>>> personsCaptor =
                ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(geminiService).generateHumanPrompt(
                personsCaptor.capture(), any(), anyBoolean(), any(), any(), any(), any());

        List<Map<String, Map<String, List<String>>>> capturedPersons = personsCaptor.getValue();

        // 1명 모드 → List 크기 1
        assertThat(capturedPersons).hasSize(1);

        // 첫 번째 인물의 stepValues: stepName → (attrName → promptValues)
        Map<String, Map<String, List<String>>> person0StepValues = capturedPersons.get(0);
        assertThat(person0StepValues).isNotEmpty();

        // 각 step 내부에 attrName이 개별 키로 분리되어 있는지 확인
        person0StepValues.forEach((stepName, attrMap) -> {
            assertThat(attrMap)
                    .as("step '%s' 의 attrMap이 비어있으면 안 됨", stepName)
                    .isNotEmpty();
            attrMap.forEach((attrName, values) -> {
                assertThat(values)
                        .as("step '%s' / attr '%s' 의 promptValues가 비어있으면 안 됨", stepName, attrName)
                        .isNotEmpty();
            });
        });

        // 개별 단계에 복수 attr가 선택된 경우 각각 별도 키로 분리되어야 함 (핵심 검증)
        boolean hasMultipleAttrsInAnyStep = person0StepValues.values().stream()
                .anyMatch(attrMap -> attrMap.size() > 1);
        assertThat(hasMultipleAttrsInAnyStep)
                .as("복수 attr가 선택된 step은 attrName이 개별 키로 분리되어야 함 (Gemini 오해 방지)")
                .isTrue();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] attrName 분리 - 공통 옵션도 attrName별로 분리되어 Gemini에 전달됨")
    @SuppressWarnings("unchecked")
    void 성공_attrName_분리_공통_옵션_Gemini_인자_구조_검증() {
        given(geminiService.generateHumanPrompt(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("common_attr_split_user", "USER", "팀CommonAttr", 5);

        // 공통 단계도 복수 attr 선택 (모든 attr 첫 option 선택)
        List<HumanPromptStepRespDto> steps = humanPromptService.getSteps();
        Map<Long, List<Long>> fullCommonSelections = new HashMap<>();
        for (HumanPromptStepRespDto step : steps) {
            if (!step.isCommon()) continue;
            for (HumanPromptStepRespDto.AttrDto attr : step.getAttrs()) {
                if (!attr.getOptions().isEmpty()) {
                    fullCommonSelections.put(attr.getId(), List.of(attr.getOptions().get(0).getId()));
                }
            }
        }

        HumanPromptCreateReqDto dto = HumanPromptCreateReqDto.builder()
                .persons(List.of(PersonSelectionDto.builder()
                        .selections(개별_최소_선택지_구성())
                        .build()))
                .commonSelections(fullCommonSelections)
                .build();

        humanPromptService.createPrompt(user.getId(), dto);

        ArgumentCaptor<Map<String, Map<String, List<String>>>> commonCaptor =
                ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(geminiService).generateHumanPrompt(
                any(), commonCaptor.capture(), anyBoolean(), any(), any(), any(), any());

        Map<String, Map<String, List<String>>> capturedCommon = commonCaptor.getValue();

        // 공통 단계가 존재하는 경우 attrName 분리 구조 확인
        assertThat(capturedCommon).isNotEmpty();
        capturedCommon.forEach((stepName, attrMap) ->
                assertThat(attrMap).as("공통 step '%s' 의 attrMap이 비어있으면 안 됨", stepName).isNotEmpty()
        );
    }

    // ================================================================
    // 이미지 생성
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이미지 생성 - 이미지URL과 최종수정자 이력에 업데이트됨")
    void 성공_이미지_생성_이미지URL_최종수정자_업데이트() {
        given(imageGenerationService.generateImages(any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of("/images/test-uuid-1.jpg", "/images/test-uuid-2.jpg", "/images/test-uuid-3.jpg"));

        UserRespDto user = 유저_생성("img_gen_user", "USER", "팀E", 5);
        PromptHistory history = PERSON_히스토리_직접_삽입(user.getId());

        List<String> imageUrls = humanPromptService.generateImage(history.getId(), MOCK_PROMPT);

        assertThat(imageUrls).hasSize(3);
        assertThat(imageUrls).contains("http://localhost:8080/images/test-uuid-1.jpg");

        PromptHistory updated = promptHistoryMapper.selectById(history.getId()).orElseThrow();
        assertThat(updated.getImageUrl()).contains("test-uuid-1");
        assertThat(updated.getFinalRvisPic()).isEqualTo("system");
        assertThat(updated.getFinalRvisDttm()).isNotNull();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이미지 생성 - 이력ID 없으면 이력 미업데이트")
    void 성공_이미지_생성_이력ID_없으면_이력_미업데이트() {
        given(imageGenerationService.generateImages(any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of("/images/test-uuid-1.jpg", "/images/test-uuid-2.jpg", "/images/test-uuid-3.jpg"));

        UserRespDto user = 유저_생성("img_gen_nohistory", "USER", "팀F", 5);
        PromptHistory history = PERSON_히스토리_직접_삽입(user.getId());

        List<String> imageUrls = humanPromptService.generateImage(null, MOCK_PROMPT);

        assertThat(imageUrls).hasSize(3);

        PromptHistory unchanged = promptHistoryMapper.selectById(history.getId()).orElseThrow();
        assertThat(unchanged.getImageUrl()).isNull();
        assertThat(unchanged.getFinalRvisPic()).isEqualTo("test");
    }
}

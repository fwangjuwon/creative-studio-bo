package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.service.CategoryService;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.GoodsPromptCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.PromptHistoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagGroupService;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagService;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * PromptService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * GeminiService는 외부 API 의존성이므로 @MockBean으로 모킹하여 테스트.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("local")
public class PromptServiceTest {

    @Autowired
    private PromptService promptService;

    @Autowired
    private PromptHistoryMapper promptHistoryMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagGroupService tagGroupService;

    @Autowired
    private TagService tagService;

    @MockBean
    private GeminiService geminiService;

    private static final String MOCK_PROMPT = "A professional product photo of high-quality Korean ginseng, studio lighting, warm tones.";

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

    private CategoryRespDto DCS_카테고리_생성(String prefix) {
        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_대분류").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_중분류").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        CategoryRespDto scs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_소분류").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());
        return categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_세분류").categoryType(CategoryType.DCS).parentId(scs.getId()).sortOrder(1).build());
    }

    private TagRespDto 태그_생성(String tagGroupName, String tagName) {
        TagGroupRespDto group = tagGroupService.createTagGroup(TagGroupCreateReqDto.builder()
                .name(tagGroupName).isMultiSelect(false).sortOrder(1).build());
        return tagService.createTag(TagCreateReqDto.builder()
                .tagGroupId(group.getId()).name(tagName).sortOrder(1).build());
    }

    private void 히스토리_직접_삽입(Long userId, Long goodsCategoryId) {
        PromptHistory history = PromptHistory.builder()
                .userId(userId)
                .categoryId(goodsCategoryId)
                .promptType("PRODUCT")
                .promptText("direct insert prompt")
                .initRegiPic("test")
                .initRegiDttm(LocalDateTime.now())
                .finalRvisPic("test")
                .finalRvisDttm(LocalDateTime.now())
                .build();
        promptHistoryMapper.insert(history);
    }

    // ================================================================
    // 프롬프트 생성 — 일일 한도 체크
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 프롬프트 생성 - 일일 한도 미만")
    void 성공_프롬프트_생성_일일한도_미만() {
        given(geminiService.generateGoodsPrompt(any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_ok", "USER", "팀A", 3);
        CategoryRespDto dcs = DCS_카테고리_생성("한도미만");
        TagRespDto tag = 태그_생성("분위기_한도미만", "화사한");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(dcs.getId())
                .tagIds(List.of(tag.getId()))
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getCategoryId()).isEqualTo(dcs.getId());
        assertThat(result.getTagIds()).containsExactly(tag.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 프롬프트 생성 - 태그 없이 카테고리만")
    void 성공_프롬프트_생성_태그없음() {
        given(geminiService.generateGoodsPrompt(any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_notag", "USER", "팀A", 5);
        CategoryRespDto dcs = DCS_카테고리_생성("태그없음");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(dcs.getId())
                .tagIds(List.of())
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getTagIds()).isEmpty();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 프롬프트 생성 - Gemini에 LCS→DCS 전체 경로 전달")
    void 성공_프롬프트_생성_카테고리_전체경로_전달() {
        given(geminiService.generateGoodsPrompt(any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_path", "USER", "팀A", 5);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("식품").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("건강식품").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        CategoryRespDto scs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("약재").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());
        CategoryRespDto dcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("인삼").categoryType(CategoryType.DCS).parentId(scs.getId()).sortOrder(1).build());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(dcs.getId())
                .tagIds(List.of())
                .build();

        promptService.createGoodsPrompt(user.getId(), dto);

        ArgumentCaptor<String> categoryNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateGoodsPrompt(categoryNameCaptor.capture(), any());

        String capturedCategoryName = categoryNameCaptor.getValue();
        assertThat(capturedCategoryName).isEqualTo("식품 > 건강식품 > 약재 > 인삼");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 프롬프트 생성 - 일일 한도 초과")
    void 예외_프롬프트_생성_일일한도_초과() {
        UserRespDto user = 유저_생성("prompt_user_over", "USER", "팀B", 2);
        CategoryRespDto dcs = DCS_카테고리_생성("한도초과");

        히스토리_직접_삽입(user.getId(), dcs.getId());
        히스토리_직접_삽입(user.getId(), dcs.getId());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(dcs.getId())
                .tagIds(List.of())
                .build();

        assertThatThrownBy(() -> promptService.createGoodsPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("[예외] 프롬프트 생성 - 존재하지 않는 사용자")
    void 예외_프롬프트_생성_사용자_미존재() {
        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(1L)
                .tagIds(List.of())
                .build();

        assertThatThrownBy(() -> promptService.createGoodsPrompt(Long.MAX_VALUE, dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.USER_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 프롬프트 생성 - 존재하지 않는 카테고리")
    void 예외_프롬프트_생성_카테고리_미존재() {
        UserRespDto user = 유저_생성("prompt_user_nocat", "USER", "팀C", 5);

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(Long.MAX_VALUE)
                .tagIds(List.of())
                .build();

        assertThatThrownBy(() -> promptService.createGoodsPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.CATEGORY_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 프롬프트 생성 - DCS가 아닌 카테고리 선택")
    void 예외_프롬프트_생성_DCS가_아닌_카테고리() {
        UserRespDto user = 유저_생성("prompt_user_lcs", "USER", "팀D", 5);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("DCS검증_대분류").categoryType(CategoryType.LCS).sortOrder(1).build());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(lcs.getId())
                .tagIds(List.of())
                .build();

        assertThatThrownBy(() -> promptService.createGoodsPrompt(user.getId(), dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.INVALID_CATEGORY_TYPE));
    }

    // ================================================================
    // 히스토리 조회 — 권한별 범위
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 - USER: 같은 팀만 조회")
    void 성공_히스토리_조회_USER_같은팀만() {
        UserRespDto userA1 = 유저_생성("hist_a1", "USER", "팀Alpha", 10);
        UserRespDto userA2 = 유저_생성("hist_a2", "USER", "팀Alpha", 10);
        UserRespDto userB  = 유저_생성("hist_b1", "USER", "팀Beta",  10);
        CategoryRespDto dcs = DCS_카테고리_생성("히스토리팀조회");

        히스토리_직접_삽입(userA1.getId(), dcs.getId());
        히스토리_직접_삽입(userA2.getId(), dcs.getId());
        히스토리_직접_삽입(userB.getId(),  dcs.getId());

        List<PromptHistoryRespDto> result = promptService.getPromptHistories(userA1.getId());

        assertThat(result).isNotEmpty();
        List<Long> userIds = result.stream().map(PromptHistoryRespDto::getUserId).toList();
        assertThat(userIds).doesNotContain(userB.getId());
        assertThat(userIds).contains(userA1.getId(), userA2.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 - MANAGER: 전체 조회")
    void 성공_히스토리_조회_MANAGER_전체() {
        UserRespDto manager = 유저_생성("hist_mgr",   "MANAGER", "관리팀", 10);
        UserRespDto userA   = 유저_생성("hist_ua",    "USER",    "팀A",   10);
        UserRespDto userB   = 유저_생성("hist_ub",    "USER",    "팀B",   10);
        CategoryRespDto dcs = DCS_카테고리_생성("MANAGER전체조회");

        히스토리_직접_삽입(userA.getId(), dcs.getId());
        히스토리_직접_삽입(userB.getId(), dcs.getId());

        List<PromptHistoryRespDto> result = promptService.getPromptHistories(manager.getId());

        List<Long> userIds = result.stream().map(PromptHistoryRespDto::getUserId).toList();
        assertThat(userIds).contains(userA.getId(), userB.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 - ADMIN: 전체 조회")
    void 성공_히스토리_조회_ADMIN_전체() {
        UserRespDto admin   = 유저_생성("hist_adm",   "ADMIN",   "관리팀", 10);
        UserRespDto userA   = 유저_생성("hist_ua2",   "USER",    "팀A",   10);
        UserRespDto userB   = 유저_생성("hist_ub2",   "USER",    "팀B",   10);
        CategoryRespDto dcs = DCS_카테고리_생성("ADMIN전체조회");

        히스토리_직접_삽입(userA.getId(), dcs.getId());
        히스토리_직접_삽입(userB.getId(), dcs.getId());

        List<PromptHistoryRespDto> result = promptService.getPromptHistories(admin.getId());

        List<Long> userIds = result.stream().map(PromptHistoryRespDto::getUserId).toList();
        assertThat(userIds).contains(userA.getId(), userB.getId());
    }

    @Test
    @DisplayName("[예외] 히스토리 조회 - 존재하지 않는 사용자")
    void 예외_히스토리_조회_사용자_미존재() {
        assertThatThrownBy(() -> promptService.getPromptHistories(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(PromptErrorCode.USER_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 - USER 팀 미배정: 빈 목록 반환")
    void 성공_히스토리_조회_USER_팀_미배정_빈목록() {
        UserRespDto userNoTeam = 유저_생성("hist_no_team", "USER", null, 10);
        CategoryRespDto dcs = DCS_카테고리_생성("팀미배정조회");

        히스토리_직접_삽입(userNoTeam.getId(), dcs.getId());

        List<PromptHistoryRespDto> result = promptService.getPromptHistories(userNoTeam.getId());

        assertThat(result).isEmpty();
    }
}

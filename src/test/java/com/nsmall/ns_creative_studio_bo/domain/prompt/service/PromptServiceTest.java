package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.page.PageReqDto;
import com.nsmall.ns_creative_studio_bo.common.page.PageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.category.service.CategoryService;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * PromptService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * GeminiService는 외부 API 의존성이므로 @MockitoBean으로 모킹하여 테스트.
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

    @MockitoBean
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

    private CategoryRespDto SCS_카테고리_생성(String prefix) {
        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_대분류").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_중분류").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        return categoryService.createCategory(CategoryCreateReqDto.builder()
                .name(prefix + "_소분류").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());
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
                .promptStyle("NANO_BANANA")
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
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_ok", "USER", "팀A", 3);
        CategoryRespDto scs = SCS_카테고리_생성("한도미만");
        TagRespDto tag = 태그_생성("분위기_한도미만", "화사한");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of(tag.getId()))
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getPromptText()).isEqualTo(MOCK_PROMPT);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getCategoryId()).isEqualTo(scs.getId());
        assertThat(result.getTagIds()).containsExactly(tag.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 프롬프트 생성 - 태그 없이 카테고리만")
    void 성공_프롬프트_생성_태그없음() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_notag", "USER", "팀A", 5);
        CategoryRespDto scs = SCS_카테고리_생성("태그없음");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of())
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result).isNotNull();
        assertThat(result.getTagIds()).isEmpty();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 프롬프트 생성 - Gemini에 LCS→SCS 전체 경로 전달")
    void 성공_프롬프트_생성_카테고리_전체경로_전달() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("prompt_user_path", "USER", "팀A", 5);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("식품").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("건강식품").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        CategoryRespDto scs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("인삼").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of())
                .build();

        promptService.createGoodsPrompt(user.getId(), dto);

        ArgumentCaptor<String> categoryNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateGoodsPrompt(categoryNameCaptor.capture(), any(), any(), any(), any());

        String capturedCategoryName = categoryNameCaptor.getValue();
        assertThat(capturedCategoryName).isEqualTo("식품 > 건강식품 > 인삼");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 프롬프트 생성 - 일일 한도 초과")
    void 예외_프롬프트_생성_일일한도_초과() {
        UserRespDto user = 유저_생성("prompt_user_over", "USER", "팀B", 2);
        CategoryRespDto scs = SCS_카테고리_생성("한도초과");

        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
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
    @DisplayName("[예외] 프롬프트 생성 - SCS가 아닌 카테고리 선택")
    void 예외_프롬프트_생성_SCS가_아닌_카테고리() {
        UserRespDto user = 유저_생성("prompt_user_lcs", "USER", "팀D", 5);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("SCS검증_대분류").categoryType(CategoryType.LCS).sortOrder(1).build());

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
    // 상품 프롬프트 생성 — 신규 필드 검증
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 상품 프롬프트 생성 - promptStyle MID_JOURNEY 명시 저장")
    void 성공_상품_프롬프트_생성_promptStyle_MID_JOURNEY() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("style_mj_user", "USER", "팀A", 5);
        CategoryRespDto scs = SCS_카테고리_생성("스타일MJ");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of())
                .promptStyle("MID_JOURNEY")
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result.getPromptStyle()).isEqualTo("MID_JOURNEY");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 상품 프롬프트 생성 - promptStyle 미입력 시 NANO_BANANA 기본값 적용")
    void 성공_상품_프롬프트_생성_promptStyle_기본값_NANO_BANANA() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("style_default_user", "USER", "팀A", 5);
        CategoryRespDto scs = SCS_카테고리_생성("스타일기본값");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of())
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result.getPromptStyle()).isEqualTo("NANO_BANANA");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 상품 프롬프트 생성 - categoryPath 전체 경로 응답 포함")
    void 성공_상품_프롬프트_생성_categoryPath_응답_포함() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("path_resp_user", "USER", "팀A", 5);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("식품").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("건강식품").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        CategoryRespDto scs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("인삼").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of())
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result.getCategoryPath()).isEqualTo("식품 > 건강식품 > 인삼");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 상품 프롬프트 생성 - tagNames 응답 포함")
    void 성공_상품_프롬프트_생성_tagNames_응답_포함() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("tagname_resp_user", "USER", "팀A", 5);
        CategoryRespDto scs = SCS_카테고리_생성("태그명응답");
        TagRespDto tag1 = 태그_생성("분위기_태그명", "화사한");
        TagRespDto tag2 = 태그_생성("인물_태그명", "여자");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of(tag1.getId(), tag2.getId()))
                .build();

        PromptHistoryRespDto result = promptService.createGoodsPrompt(user.getId(), dto);

        assertThat(result.getTagNames()).containsExactlyInAnyOrder("화사한", "여자");
    }

    // ================================================================
    // 히스토리 조회 — 권한별 범위
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 - ADMIN/MANAGER/USER 모두 동일한 전체 결과 반환")
    void 성공_히스토리_조회_전체_권한별_동일결과() {
        UserRespDto admin   = 유저_생성("hist_role_adm", "ADMIN",   "관리팀", 10);
        UserRespDto manager = 유저_생성("hist_role_mgr", "MANAGER", "운영팀", 10);
        UserRespDto user    = 유저_생성("hist_role_usr", "USER",    "팀A",   10);
        CategoryRespDto scs = SCS_카테고리_생성("권한별동일결과");

        히스토리_직접_삽입(admin.getId(),   scs.getId());
        히스토리_직접_삽입(manager.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(),    scs.getId());

        PageReqDto pageReqDto = new PageReqDto(1, 100);

        List<Long> result1 = promptService.getPromptHistories(pageReqDto).items().stream()
                .map(PromptHistoryRespDto::getId).toList();
        List<Long> result2 = promptService.getPromptHistories(pageReqDto).items().stream()
                .map(PromptHistoryRespDto::getId).toList();
        List<Long> result3 = promptService.getPromptHistories(pageReqDto).items().stream()
                .map(PromptHistoryRespDto::getId).toList();

        // 세 번 호출해도 항상 동일한 결과 — Role 기반 필터가 존재하지 않음을 증명
        assertThat(result1).containsExactlyInAnyOrderElementsOf(result2);
        assertThat(result1).containsExactlyInAnyOrderElementsOf(result3);

        // 세 Role의 히스토리가 모두 포함되어 있는지 검증
        List<Long> histUserIds = promptService.getPromptHistories(pageReqDto).items().stream()
                .map(PromptHistoryRespDto::getUserId).toList();
        assertThat(histUserIds).contains(admin.getId(), manager.getId(), user.getId());
    }

    // ================================================================
    // 이력 조회 — PRODUCT 신규 필드 검증
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 이력 조회 - PRODUCT 이력에 categoryPath/tagNames/promptStyle 포함")
    void 성공_이력_조회_PRODUCT_신규필드_포함() {
        given(geminiService.generateGoodsPrompt(any(), any(), any(), any(), any())).willReturn(MOCK_PROMPT);

        UserRespDto user = 유저_생성("hist_detail_user", "USER", "팀Detail", 10);

        CategoryRespDto lcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("의류").categoryType(CategoryType.LCS).sortOrder(1).build());
        CategoryRespDto mcs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("상의").categoryType(CategoryType.MCS).parentId(lcs.getId()).sortOrder(1).build());
        CategoryRespDto scs = categoryService.createCategory(CategoryCreateReqDto.builder()
                .name("반팔").categoryType(CategoryType.SCS).parentId(mcs.getId()).sortOrder(1).build());

        TagRespDto tag = 태그_생성("배경_이력상세", "야외");

        GoodsPromptCreateReqDto dto = GoodsPromptCreateReqDto.builder()
                .goodsCategoryId(scs.getId())
                .tagIds(List.of(tag.getId()))
                .promptStyle("MID_JOURNEY")
                .build();

        promptService.createGoodsPrompt(user.getId(), dto);

        PageRespDto<PromptHistoryRespDto> page = promptService.getPromptHistories(new PageReqDto(1, 100));
        PromptHistoryRespDto found = page.items().stream()
                .filter(h -> "PRODUCT".equals(h.getPromptType()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getCategoryPath()).isEqualTo("의류 > 상의 > 반팔");
        assertThat(found.getTagNames()).containsExactly("야외");
        assertThat(found.getPromptStyle()).isEqualTo("MID_JOURNEY");
    }

    // ================================================================
    // 히스토리 조회 — 페이징
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 페이징 - 첫 페이지 메타정보 검증")
    void 성공_히스토리_조회_페이징_첫페이지_메타정보() {
        UserRespDto user = 유저_생성("paging_meta_user", "USER", "팀P", 10);
        CategoryRespDto scs = SCS_카테고리_생성("페이징메타");

        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());

        PageRespDto<PromptHistoryRespDto> result = promptService.getPromptHistories(new PageReqDto(1, 10));

        assertThat(result).isNotNull();
        assertThat(result.pageNum()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.total()).isGreaterThanOrEqualTo(3L);
        assertThat(result.hasPreviousPage()).isFalse();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 페이징 - pageSize 제한 적용 확인")
    void 성공_히스토리_조회_페이징_pageSize_제한() {
        UserRespDto user = 유저_생성("paging_limit_user", "USER", "팀Q", 10);
        CategoryRespDto scs = SCS_카테고리_생성("페이징제한");

        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());

        PageRespDto<PromptHistoryRespDto> result = promptService.getPromptHistories(new PageReqDto(1, 1));

        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isGreaterThanOrEqualTo(3L);
        assertThat(result.hasNextPage()).isTrue();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 히스토리 조회 페이징 - 빈 결과")
    void 성공_히스토리_조회_페이징_빈결과() {
        UserRespDto user = 유저_생성("paging_empty_user", "USER", "팀R", 10);
        CategoryRespDto scs = SCS_카테고리_생성("페이징빈결과");

        히스토리_직접_삽입(user.getId(), scs.getId());
        히스토리_직접_삽입(user.getId(), scs.getId());

        // 전체 2건이므로 pageSize=2, pageNum=2 → 2페이지는 항상 빈 결과
        PageRespDto<PromptHistoryRespDto> result = promptService.getPromptHistories(new PageReqDto(2, 1000));

        assertThat(result).isNotNull();
        assertThat(result.items()).isEmpty();
        assertThat(result.hasPreviousPage()).isTrue();
        assertThat(result.hasNextPage()).isFalse();
    }

    // ================================================================
    // 상품 이미지 생성 — 참조 이미지 검증
    // ================================================================

    @Test
    @DisplayName("[예외] 상품 이미지 생성 - 허용되지 않는 MIME 타입 (text/plain)")
    void 예외_상품_이미지_생성_허용되지않는_MIME타입() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn("text/plain");

        assertThatThrownBy(() -> promptService.generateGoodsImage(null, "test prompt", file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }

    @Test
    @DisplayName("[예외] 상품 이미지 생성 - 허용되지 않는 MIME 타입 (application/x-sh)")
    void 예외_상품_이미지_생성_허용되지않는_MIME타입_sh() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn("application/x-sh");

        assertThatThrownBy(() -> promptService.generateGoodsImage(null, "test prompt", file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }

    @Test
    @DisplayName("[예외] 상품 이미지 생성 - Content-Type null")
    void 예외_상품_이미지_생성_ContentType_null() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(1024L);
        given(file.getContentType()).willReturn(null);

        assertThatThrownBy(() -> promptService.generateGoodsImage(null, "test prompt", file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.INVALID_IMAGE_MIME_TYPE));
    }

    @Test
    @DisplayName("[예외] 상품 이미지 생성 - 파일 크기 10MB 초과")
    void 예외_상품_이미지_생성_파일크기_초과() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(11 * 1024 * 1024L); // 11MB

        assertThatThrownBy(() -> promptService.generateGoodsImage(null, "test prompt", file))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ImageGenerationErrorCode.IMAGE_TOO_LARGE));
    }
}

package com.nsmall.ns_creative_studio_bo.domain.category.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryType;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.request.CategoryUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.category.dto.response.CategoryRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CategoryService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("local")
public class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PromptHistoryMapper promptHistoryMapper;

    private CategoryCreateReqDto LCS_생성_DTO(String name) {
        return CategoryCreateReqDto.builder()
                .name(name)
                .categoryType(CategoryType.LCS)
                .parentId(null)
                .sortOrder(1)
                .build();
    }

    private CategoryCreateReqDto 하위_생성_DTO(String name, CategoryType categoryType, Long parentId) {
        return CategoryCreateReqDto.builder()
                .name(name)
                .categoryType(categoryType)
                .parentId(parentId)
                .sortOrder(1)
                .build();
    }

    // ============================================================
    // 카테고리 생성
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 생성 - LCS (parentId null)")
    void 성공_카테고리_생성_LCS() {
        CategoryRespDto result = categoryService.createCategory(LCS_생성_DTO("테스트대분류"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트대분류");
        assertThat(result.getCategoryType()).isEqualTo(CategoryType.LCS);
        assertThat(result.getParentId()).isNull();
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 생성 - 4단계 정상 계층 (LCS→MCS→SCS→DCS)")
    void 성공_카테고리_생성_4단계_정상계층() {
        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("부모대분류"));
        CategoryRespDto mcs = categoryService.createCategory(하위_생성_DTO("중분류", CategoryType.MCS, lcs.getId()));
        CategoryRespDto scs = categoryService.createCategory(하위_생성_DTO("소분류", CategoryType.SCS, mcs.getId()));
        CategoryRespDto dcs = categoryService.createCategory(하위_생성_DTO("세분류", CategoryType.DCS, scs.getId()));

        assertThat(mcs.getCategoryType()).isEqualTo(CategoryType.MCS);
        assertThat(scs.getCategoryType()).isEqualTo(CategoryType.SCS);
        assertThat(dcs.getCategoryType()).isEqualTo(CategoryType.DCS);
        assertThat(dcs.getParentId()).isEqualTo(scs.getId());
    }

    @Test
    @DisplayName("[예외] 카테고리 생성 - LCS인데 parentId 입력")
    void 예외_카테고리_생성_LCS에_parentId_입력() {
        CategoryCreateReqDto dto = CategoryCreateReqDto.builder()
                .name("잘못된대분류")
                .categoryType(CategoryType.LCS)
                .parentId(1L)
                .build();

        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY));
    }

    @ParameterizedTest
    @EnumSource(value = CategoryType.class, names = {"MCS", "SCS", "DCS"})
    @DisplayName("[예외] 카테고리 생성 - MCS/SCS/DCS인데 parentId null")
    void 예외_카테고리_생성_하위_카테고리에_parentId_없음(CategoryType categoryType) {
        CategoryCreateReqDto dto = CategoryCreateReqDto.builder()
                .name("부모없는자식")
                .categoryType(categoryType)
                .parentId(null)
                .build();

        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY));
    }

    @ParameterizedTest
    @CsvSource({
            "MCS, MCS",
            "MCS, SCS",
            "MCS, DCS",
            "SCS, LCS",
            "SCS, SCS",
            "SCS, DCS",
            "DCS, LCS",
            "DCS, MCS",
            "DCS, DCS"
    })
    @Transactional
    @Rollback
    @DisplayName("[예외] 카테고리 생성 - 잘못된 부모 타입 (계층 순서 위반)")
    void 예외_카테고리_생성_잘못된_부모_타입(String childTypeStr, String wrongParentTypeStr) {
        CategoryType childType = CategoryType.valueOf(childTypeStr);
        CategoryType wrongParentType = CategoryType.valueOf(wrongParentTypeStr);

        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("계층위반_LCS"));

        Long wrongParentId = lcs.getId();
        if (List.of(CategoryType.MCS, CategoryType.SCS, CategoryType.DCS).contains(wrongParentType)) {
            CategoryRespDto mcs = categoryService.createCategory(하위_생성_DTO("계층위반_MCS", CategoryType.MCS, lcs.getId()));
            wrongParentId = mcs.getId();
        }
        if (List.of(CategoryType.SCS, CategoryType.DCS).contains(wrongParentType)) {
            CategoryRespDto scs = categoryService.createCategory(하위_생성_DTO("계층위반_SCS", CategoryType.SCS, wrongParentId));
            wrongParentId = scs.getId();
        }
        if (CategoryType.DCS == wrongParentType) {
            CategoryRespDto dcs = categoryService.createCategory(하위_생성_DTO("계층위반_DCS", CategoryType.DCS, wrongParentId));
            wrongParentId = dcs.getId();
        }

        final Long finalWrongParentId = wrongParentId;
        assertThatThrownBy(() -> categoryService.createCategory(하위_생성_DTO("잘못된자식", childType, finalWrongParentId)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.INVALID_CATEGORY_HIERARCHY));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 카테고리 생성 - 상위 카테고리 미존재")
    void 예외_카테고리_생성_상위_카테고리_미존재() {
        CategoryCreateReqDto dto = 하위_생성_DTO("자식카테고리", CategoryType.MCS, Long.MAX_VALUE);

        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // 카테고리 단건 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 단건 조회 - 존재하는 카테고리")
    void 성공_카테고리_단건_조회_존재() {
        CategoryRespDto created = categoryService.createCategory(LCS_생성_DTO("단건조회테스트"));

        Optional<CategoryRespDto> result = categoryService.getCategory(created.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("단건조회테스트");
        assertThat(result.get().getCategoryType()).isEqualTo(CategoryType.LCS);
    }

    @Test
    @DisplayName("[없음] 카테고리 단건 조회 - 미존재 카테고리")
    void 없음_카테고리_단건_조회_미존재() {
        Optional<CategoryRespDto> result = categoryService.getCategory(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // ============================================================
    // 카테고리 목록 조회 (드릴다운)
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 목록 조회 - LCS 전체 목록 (parentId null)")
    void 성공_카테고리_목록_조회_LCS_전체() {
        categoryService.createCategory(LCS_생성_DTO("대분류A"));
        categoryService.createCategory(LCS_생성_DTO("대분류B"));

        List<CategoryRespDto> result = categoryService.getCategories(null);

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(2);
        assertThat(result).allSatisfy(c -> assertThat(c.getParentId()).isNull());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 목록 조회 - 하위 카테고리 (parentId 지정)")
    void 성공_카테고리_목록_조회_하위() {
        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("드릴다운대분류"));
        categoryService.createCategory(하위_생성_DTO("중분류A", CategoryType.MCS, lcs.getId()));
        categoryService.createCategory(하위_생성_DTO("중분류B", CategoryType.MCS, lcs.getId()));

        List<CategoryRespDto> result = categoryService.getCategories(lcs.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(c -> {
            assertThat(c.getParentId()).isEqualTo(lcs.getId());
            assertThat(c.getCategoryType()).isEqualTo(CategoryType.MCS);
        });
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 목록 조회 - 4단계 전체 드릴다운 (LCS→MCS→SCS→DCS)")
    void 성공_카테고리_목록_조회_4단계_드릴다운() {
        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("식품"));
        CategoryRespDto mcs = categoryService.createCategory(하위_생성_DTO("건강식품", CategoryType.MCS, lcs.getId()));
        CategoryRespDto scs = categoryService.createCategory(하위_생성_DTO("약재", CategoryType.SCS, mcs.getId()));
        CategoryRespDto dcs = categoryService.createCategory(하위_생성_DTO("인삼", CategoryType.DCS, scs.getId()));

        assertThat(categoryService.getCategories(null))
                .anyMatch(c -> c.getId().equals(lcs.getId()));
        assertThat(categoryService.getCategories(lcs.getId()))
                .anyMatch(c -> c.getId().equals(mcs.getId()));
        assertThat(categoryService.getCategories(mcs.getId()))
                .anyMatch(c -> c.getId().equals(scs.getId()));
        assertThat(categoryService.getCategories(scs.getId()))
                .anyMatch(c -> c.getId().equals(dcs.getId()));
    }

    // ============================================================
    // 카테고리 수정
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 수정 - 이름 / 정렬순서 변경")
    void 성공_카테고리_수정() {
        CategoryRespDto created = categoryService.createCategory(LCS_생성_DTO("수정전대분류"));

        CategoryUpdateReqDto updateDto = CategoryUpdateReqDto.builder()
                .name("수정후대분류")
                .sortOrder(99)
                .build();

        CategoryRespDto updated = categoryService.updateCategory(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정후대분류");
        assertThat(updated.getSortOrder()).isEqualTo(99);
        assertThat(updated.getCategoryType()).isEqualTo(CategoryType.LCS);
        assertThat(updated.getParentId()).isNull();
    }

    @Test
    @DisplayName("[예외] 카테고리 수정 - 미존재 카테고리")
    void 예외_카테고리_수정_미존재() {
        CategoryUpdateReqDto updateDto = CategoryUpdateReqDto.builder().name("수정요청").build();

        assertThatThrownBy(() -> categoryService.updateCategory(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // 카테고리 삭제
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 카테고리 삭제 - 삭제 후 조회 시 미존재")
    void 성공_카테고리_삭제() {
        CategoryRespDto created = categoryService.createCategory(LCS_생성_DTO("삭제테스트대분류"));

        categoryService.deleteCategory(created.getId());

        Optional<CategoryRespDto> result = categoryService.getCategory(created.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[예외] 카테고리 삭제 - 미존재 카테고리")
    void 예외_카테고리_삭제_미존재() {
        assertThatThrownBy(() -> categoryService.deleteCategory(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 카테고리 삭제 - 하위 카테고리 존재")
    void 예외_카테고리_삭제_하위_카테고리_존재() {
        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("삭제불가대분류"));
        categoryService.createCategory(하위_생성_DTO("삭제불가중분류", CategoryType.MCS, lcs.getId()));

        assertThatThrownBy(() -> categoryService.deleteCategory(lcs.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.CATEGORY_HAS_CHILDREN));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 카테고리 삭제 - 프롬프트 히스토리에서 사용 중")
    void 예외_카테고리_삭제_사용_중() {
        CategoryRespDto lcs = categoryService.createCategory(LCS_생성_DTO("사용중_대분류"));
        CategoryRespDto mcs = categoryService.createCategory(하위_생성_DTO("사용중_중분류", CategoryType.MCS, lcs.getId()));
        CategoryRespDto scs = categoryService.createCategory(하위_생성_DTO("사용중_소분류", CategoryType.SCS, mcs.getId()));
        CategoryRespDto dcs = categoryService.createCategory(하위_생성_DTO("사용중_세분류", CategoryType.DCS, scs.getId()));

        LocalDateTime now = LocalDateTime.now();
        promptHistoryMapper.insert(PromptHistory.builder()
                .userId(0L)
                .categoryId(dcs.getId())
                .promptType("PRODUCT")
                .promptText("테스트 프롬프트")
                .initRegiPic("test_user")
                .initRegiDttm(now)
                .finalRvisPic("test_user")
                .finalRvisDttm(now)
                .build());

        assertThatThrownBy(() -> categoryService.deleteCategory(dcs.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(CategoryErrorCode.CATEGORY_IN_USE));
    }
}

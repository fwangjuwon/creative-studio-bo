package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptAttrSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepSimpleRespDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HumanPromptAttrService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * Loop 1에서 미뤄진 "[예외] Step 삭제 - 하위 Attr 존재 시 거부" 케이스도 여기서 검증한다.
 * "[예외] Attr 삭제 - 하위 Option 존재 시 거부" 케이스는 HumanPromptOptionService(Loop 3)
 * 구현 완료 후 HumanPromptOptionServiceTest에서 함께 검증한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("local")
public class HumanPromptAttrServiceTest {

    @Autowired
    private HumanPromptAttrService humanPromptAttrService;

    @Autowired
    private HumanPromptStepService humanPromptStepService;

    private HumanPromptAttrCreateReqDto 세부항목_생성_DTO(Long stepId, String name, boolean multiSelect, boolean required, int sortOrder) {
        return HumanPromptAttrCreateReqDto.builder()
                .stepId(stepId)
                .name(name)
                .isMultiSelect(multiSelect)
                .isRequired(required)
                .sortOrder(sortOrder)
                .build();
    }

    private HumanPromptStepSimpleRespDto 단계_생성(int stepNumber, String stepCode, String name) {
        return humanPromptStepService.createStep(
                HumanPromptStepCreateReqDto.builder()
                        .stepNumber(stepNumber)
                        .stepCode(stepCode)
                        .name(name)
                        .build()
        );
    }

    // ================================================================
    // Attr 생성
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Attr 생성")
    void 성공_Attr_생성() {
        HumanPromptStepSimpleRespDto step = 단계_생성(10, "X", "테스트 단계");

        HumanPromptAttrSimpleRespDto result =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "테스트 속성", false, true, 1));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStepId()).isEqualTo(step.getId());
        assertThat(result.getName()).isEqualTo("테스트 속성");
        assertThat(result.isMultiSelect()).isFalse();
        assertThat(result.isRequired()).isTrue();
        assertThat(result.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("[예외] Attr 생성 - 존재하지 않는 단계")
    void 예외_Attr_생성_미존재_단계() {
        assertThatThrownBy(() ->
                humanPromptAttrService.createAttr(세부항목_생성_DTO(Long.MAX_VALUE, "속성명", false, true, 1)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.STEP_NOT_FOUND));
    }

    // ================================================================
    // Attr 수정
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Attr 수정 - name / isMultiSelect / isRequired / sortOrder 변경")
    void 성공_Attr_수정() {
        HumanPromptStepSimpleRespDto step = 단계_생성(11, "Y", "수정테스트 단계");
        HumanPromptAttrSimpleRespDto created =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "수정전 속성", false, false, 1));

        HumanPromptAttrUpdateReqDto updateDto = HumanPromptAttrUpdateReqDto.builder()
                .name("수정후 속성")
                .isMultiSelect(true)
                .isRequired(true)
                .sortOrder(99)
                .build();

        HumanPromptAttrSimpleRespDto updated =
                humanPromptAttrService.updateAttr(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정후 속성");
        assertThat(updated.isMultiSelect()).isTrue();
        assertThat(updated.isRequired()).isTrue();
        assertThat(updated.getSortOrder()).isEqualTo(99);
        assertThat(updated.getStepId()).isEqualTo(step.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] Attr 수정 - 마지막 필수 항목을 비필수로 변경 시 거부")
    void 예외_Attr_수정_마지막_필수항목_해제_거부() {
        HumanPromptStepSimpleRespDto step = 단계_생성(14, "V", "필수검증 단계");
        HumanPromptAttrSimpleRespDto required =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "유일한 필수 속성", false, true, 1));

        HumanPromptAttrUpdateReqDto updateDto = HumanPromptAttrUpdateReqDto.builder()
                .name("유일한 필수 속성")
                .isMultiSelect(false)
                .isRequired(false)   // ← 마지막 필수를 비필수로
                .sortOrder(1)
                .build();

        assertThatThrownBy(() -> humanPromptAttrService.updateAttr(required.getId(), updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_LAST_REQUIRED));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Attr 수정 - 필수 항목 해제 (다른 필수 항목 존재 시 허용)")
    void 성공_Attr_수정_필수해제_다른_필수_존재() {
        HumanPromptStepSimpleRespDto step = 단계_생성(15, "U", "필수복수 단계");
        HumanPromptAttrSimpleRespDto required1 =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "필수 속성1", false, true, 1));
        humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "필수 속성2", false, true, 2));

        HumanPromptAttrUpdateReqDto updateDto = HumanPromptAttrUpdateReqDto.builder()
                .name("필수 속성1")
                .isMultiSelect(false)
                .isRequired(false)   // ← 다른 필수 attr이 있으므로 허용
                .sortOrder(1)
                .build();

        HumanPromptAttrSimpleRespDto updated = humanPromptAttrService.updateAttr(required1.getId(), updateDto);
        assertThat(updated.isRequired()).isFalse();
    }

    @Test
    @DisplayName("[예외] Attr 수정 - 미존재")
    void 예외_Attr_수정_미존재() {
        HumanPromptAttrUpdateReqDto updateDto = HumanPromptAttrUpdateReqDto.builder()
                .name("수정요청")
                .isMultiSelect(false)
                .isRequired(false)
                .sortOrder(1)
                .build();

        assertThatThrownBy(() -> humanPromptAttrService.updateAttr(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_NOT_FOUND));
    }

    // ================================================================
    // Attr 삭제
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Attr 삭제 - 하위 Option 없을 때")
    void 성공_Attr_삭제_하위Option_없음() {
        HumanPromptStepSimpleRespDto step = 단계_생성(12, "Z", "삭제테스트 단계");
        HumanPromptAttrSimpleRespDto created =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "삭제가능 속성", false, false, 1));

        humanPromptAttrService.deleteAttr(created.getId());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] Attr 삭제 - 마지막 필수 항목 삭제 거부")
    void 예외_Attr_삭제_마지막_필수항목_삭제_거부() {
        HumanPromptStepSimpleRespDto step = 단계_생성(16, "T", "삭제필수검증 단계");
        HumanPromptAttrSimpleRespDto required =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "유일한 필수 속성", false, true, 1));

        assertThatThrownBy(() -> humanPromptAttrService.deleteAttr(required.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_LAST_REQUIRED));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Attr 삭제 - 필수 항목 삭제 (다른 필수 항목 존재 시 허용)")
    void 성공_Attr_삭제_필수항목_다른_필수_존재() {
        HumanPromptStepSimpleRespDto step = 단계_생성(17, "S2", "삭제필수복수 단계");
        HumanPromptAttrSimpleRespDto required1 =
                humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "삭제할 필수 속성", false, true, 1));
        humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "남은 필수 속성", false, true, 2));

        // 다른 필수 attr이 있으므로 삭제 허용
        humanPromptAttrService.deleteAttr(required1.getId());
    }

    @Test
    @DisplayName("[예외] Attr 삭제 - 미존재")
    void 예외_Attr_삭제_미존재() {
        assertThatThrownBy(() -> humanPromptAttrService.deleteAttr(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_NOT_FOUND));
    }

    // ================================================================
    // Step 삭제 — 하위 Attr 존재 시 거부 (Loop 1에서 이월)
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] Step 삭제 - 하위 Attr 존재 시 거부")
    void 예외_Step_삭제_하위Attr_존재() {
        HumanPromptStepSimpleRespDto step = 단계_생성(13, "W", "삭제거부 단계");
        humanPromptAttrService.createAttr(세부항목_생성_DTO(step.getId(), "하위 속성", false, false, 1));

        assertThatThrownBy(() -> humanPromptStepService.deleteStep(step.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.STEP_HAS_ATTRS));
    }
}

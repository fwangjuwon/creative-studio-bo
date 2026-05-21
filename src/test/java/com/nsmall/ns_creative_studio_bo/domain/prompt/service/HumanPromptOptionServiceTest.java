package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptAttrCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptOptionUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptAttrSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptOptionSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepSimpleRespDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.HumanPromptHistoryOption;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.HumanPromptHistoryOptionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HumanPromptOptionService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * Loop 2에서 미뤄진 "[예외] Attr 삭제 - 하위 Option 존재 시 거부" 케이스도 여기서 검증한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("local")
public class HumanPromptOptionServiceTest {

    @Autowired
    private HumanPromptOptionService humanPromptOptionService;

    @Autowired
    private HumanPromptAttrService humanPromptAttrService;

    @Autowired
    private HumanPromptStepService humanPromptStepService;

    @Autowired
    private HumanPromptHistoryOptionMapper humanPromptHistoryOptionMapper;

    private HumanPromptStepSimpleRespDto 단계_생성(int stepNumber, String stepCode, String name) {
        return humanPromptStepService.createStep(
                HumanPromptStepCreateReqDto.builder()
                        .stepNumber(stepNumber)
                        .stepCode(stepCode)
                        .name(name)
                        .build()
        );
    }

    private HumanPromptAttrSimpleRespDto 세부항목_생성(Long stepId, String name) {
        return humanPromptAttrService.createAttr(
                HumanPromptAttrCreateReqDto.builder()
                        .stepId(stepId)
                        .name(name)
                        .isMultiSelect(false)
                        .isRequired(true)
                        .sortOrder(1)
                        .build()
        );
    }

    private HumanPromptOptionSimpleRespDto 선택지_생성(Long attrId, String name, String promptValue) {
        return humanPromptOptionService.createOption(
                HumanPromptOptionCreateReqDto.builder()
                        .attrId(attrId)
                        .name(name)
                        .promptValue(promptValue)
                        .sortOrder(1)
                        .build()
        );
    }

    // ================================================================
    // Option 생성
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Option 생성 - sortOrder 미입력 시 attr 내 최대값 + 1 자동 세팅")
    void 성공_Option_생성_sortOrder_자동증가() {
        HumanPromptStepSimpleRespDto step = 단계_생성(21, "Q", "자동증가테스트단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "자동증가테스트속성");
        선택지_생성(attr.getId(), "기준선택지", "base_value");  // sortOrder=1 기준

        HumanPromptOptionSimpleRespDto result = humanPromptOptionService.createOption(
                HumanPromptOptionCreateReqDto.builder()
                        .attrId(attr.getId())
                        .name("자동선택지")
                        .promptValue("auto_value")
                        .build());  // sortOrder null

        assertThat(result.getSortOrder()).isEqualTo(2);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Option 생성 - sortOrder 연속 미입력 시 순차 증가")
    void 성공_Option_생성_sortOrder_연속_자동증가() {
        HumanPromptStepSimpleRespDto step = 단계_생성(22, "R", "연속증가테스트단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "연속증가테스트속성");
        선택지_생성(attr.getId(), "기준선택지", "base_value");  // sortOrder=1 기준

        HumanPromptOptionSimpleRespDto result1 = humanPromptOptionService.createOption(
                HumanPromptOptionCreateReqDto.builder()
                        .attrId(attr.getId()).name("자동선택지A").promptValue("auto_a").build());
        HumanPromptOptionSimpleRespDto result2 = humanPromptOptionService.createOption(
                HumanPromptOptionCreateReqDto.builder()
                        .attrId(attr.getId()).name("자동선택지B").promptValue("auto_b").build());

        assertThat(result1.getSortOrder()).isEqualTo(2);
        assertThat(result2.getSortOrder()).isEqualTo(3);
    }



    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Option 생성")
    void 성공_Option_생성() {
        HumanPromptStepSimpleRespDto step = 단계_생성(20, "P", "테스트 단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "테스트 속성");

        HumanPromptOptionSimpleRespDto result = 선택지_생성(attr.getId(), "남성", "male");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getAttrId()).isEqualTo(attr.getId());
        assertThat(result.getName()).isEqualTo("남성");
        assertThat(result.getPromptValue()).isEqualTo("male");
        assertThat(result.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("[예외] Option 생성 - 존재하지 않는 세부항목")
    void 예외_Option_생성_미존재_세부항목() {
        assertThatThrownBy(() -> 선택지_생성(Long.MAX_VALUE, "남성", "male"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_NOT_FOUND));
    }

    // ================================================================
    // Option 수정
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Option 수정 - name / promptValue / sortOrder 변경")
    void 성공_Option_수정() {
        HumanPromptStepSimpleRespDto step = 단계_생성(21, "Q", "수정테스트 단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "수정테스트 속성");
        HumanPromptOptionSimpleRespDto created = 선택지_생성(attr.getId(), "수정전 선택지", "before_value");

        HumanPromptOptionUpdateReqDto updateDto = HumanPromptOptionUpdateReqDto.builder()
                .name("수정후 선택지")
                .promptValue("after_value")
                .sortOrder(99)
                .build();

        HumanPromptOptionSimpleRespDto updated =
                humanPromptOptionService.updateOption(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정후 선택지");
        assertThat(updated.getPromptValue()).isEqualTo("after_value");
        assertThat(updated.getSortOrder()).isEqualTo(99);
        assertThat(updated.getAttrId()).isEqualTo(attr.getId());
    }

    @Test
    @DisplayName("[예외] Option 수정 - 미존재")
    void 예외_Option_수정_미존재() {
        HumanPromptOptionUpdateReqDto updateDto = HumanPromptOptionUpdateReqDto.builder()
                .name("수정요청")
                .promptValue("value")
                .sortOrder(1)
                .build();

        assertThatThrownBy(() -> humanPromptOptionService.updateOption(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.OPTION_NOT_FOUND));
    }

    // ================================================================
    // Option 삭제
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Option 삭제 - 히스토리 미참조")
    void 성공_Option_삭제_히스토리_미참조() {
        HumanPromptStepSimpleRespDto step = 단계_생성(22, "R", "삭제테스트 단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "삭제테스트 속성");
        HumanPromptOptionSimpleRespDto created = 선택지_생성(attr.getId(), "삭제가능 선택지", "deletable");

        humanPromptOptionService.deleteOption(created.getId());
    }

    @Test
    @DisplayName("[예외] Option 삭제 - 미존재")
    void 예외_Option_삭제_미존재() {
        assertThatThrownBy(() -> humanPromptOptionService.deleteOption(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.OPTION_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] Option 삭제 - 히스토리 사용 중")
    void 예외_Option_삭제_히스토리_사용중() {
        HumanPromptStepSimpleRespDto step = 단계_생성(23, "S", "이력참조 단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "이력참조 속성");
        HumanPromptOptionSimpleRespDto option = 선택지_생성(attr.getId(), "이력참조 선택지", "in_use_value");

        // DB에 FK 제약 없으므로 존재하지 않는 prompt_history_id(9999L) 사용 가능
        LocalDateTime now = LocalDateTime.now();
        humanPromptHistoryOptionMapper.insertBatch(List.of(
                HumanPromptHistoryOption.builder()
                        .promptHistoryId(9999L)
                        .optionId(option.getId())
                        .personIndex(0)
                        .initRegiPic("test")
                        .initRegiDttm(now)
                        .finalRvisPic("test")
                        .finalRvisDttm(now)
                        .build()
        ));

        assertThatThrownBy(() -> humanPromptOptionService.deleteOption(option.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.OPTION_IN_USE));
    }

    // ================================================================
    // Attr 삭제 — 하위 Option 존재 시 거부 (Loop 2에서 이월)
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] Attr 삭제 - 하위 Option 존재 시 거부")
    void 예외_Attr_삭제_하위Option_존재() {
        HumanPromptStepSimpleRespDto step = 단계_생성(24, "T", "Attr삭제거부 단계");
        HumanPromptAttrSimpleRespDto attr = 세부항목_생성(step.getId(), "삭제거부 속성");
        선택지_생성(attr.getId(), "하위 선택지", "sub_option");

        assertThatThrownBy(() -> humanPromptAttrService.deleteAttr(attr.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.ATTR_HAS_OPTIONS));
    }
}

package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.HumanPromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request.HumanPromptStepUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response.HumanPromptStepSimpleRespDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HumanPromptStepService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * <pre>
 * "[예외] Step 삭제 - 하위 Attr 존재 시 거부" 케이스는 HumanPromptAttrService(Loop 2)
 * 구현 완료 후 HumanPromptAttrServiceTest에서 함께 검증한다.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("local")
public class HumanPromptStepServiceTest {

    /** 테스트 실행마다 고유한 접미사 — step_code는 VARCHAR(2) 제약으로 1자리 hex char만 사용 */
    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 6);
    private static final String CODE_SUFFIX = UUID.randomUUID().toString().substring(0, 1);

    @Autowired
    private HumanPromptStepService humanPromptStepService;

    private HumanPromptStepCreateReqDto 단계_생성_DTO(int stepNumber, String stepCode, String name) {
        return HumanPromptStepCreateReqDto.builder()
                .stepNumber(stepNumber)
                .stepCode(stepCode)
                .name(name)
                .build();
    }

    // ================================================================
    // Step 생성
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Step 생성")
    void 성공_Step_생성() {
        String code = "F" + CODE_SUFFIX;
        String name = "테스트 단계_" + RUN_ID;

        HumanPromptStepSimpleRespDto result =
                humanPromptStepService.createStep(단계_생성_DTO(6, code, name));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStepNumber()).isEqualTo(6);
        assertThat(result.getStepCode()).isEqualTo(code);
        assertThat(result.getName()).isEqualTo(name);
    }

    // ================================================================
    // Step 수정
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Step 수정 - stepNumber / name 변경")
    void 성공_Step_수정_stepNumber_name_변경() {
        String code = "G" + CODE_SUFFIX;
        String beforeName = "수정전 단계_" + RUN_ID;
        String afterName = "수정후 단계_" + RUN_ID;

        HumanPromptStepSimpleRespDto created =
                humanPromptStepService.createStep(단계_생성_DTO(7, code, beforeName));

        HumanPromptStepUpdateReqDto updateDto = HumanPromptStepUpdateReqDto.builder()
                .stepNumber(99)
                .stepCode(code)
                .name(afterName)
                .build();

        HumanPromptStepSimpleRespDto updated =
                humanPromptStepService.updateStep(created.getId(), updateDto);

        assertThat(updated.getStepNumber()).isEqualTo(99);
        assertThat(updated.getName()).isEqualTo(afterName);
        assertThat(updated.getStepCode()).isEqualTo(code.toUpperCase()); // 서비스에서 toUpperCase() 처리
    }

    @Test
    @DisplayName("[예외] Step 수정 - 미존재")
    void 예외_Step_수정_미존재() {
        HumanPromptStepUpdateReqDto updateDto = HumanPromptStepUpdateReqDto.builder()
                .stepNumber(1)
                .stepCode("Z")
                .name("수정요청")
                .build();

        assertThatThrownBy(() -> humanPromptStepService.updateStep(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.STEP_NOT_FOUND));
    }

    // ================================================================
    // Step 삭제
    // ================================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] Step 삭제 - 하위 Attr 없을 때")
    void 성공_Step_삭제_하위Attr_없음() {
        String code = "H" + CODE_SUFFIX;
        String name = "삭제가능 단계_" + RUN_ID;

        HumanPromptStepSimpleRespDto created =
                humanPromptStepService.createStep(단계_생성_DTO(8, code, name));

        humanPromptStepService.deleteStep(created.getId());
    }

    @Test
    @DisplayName("[예외] Step 삭제 - 미존재")
    void 예외_Step_삭제_미존재() {
        assertThatThrownBy(() -> humanPromptStepService.deleteStep(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(HumanPromptErrorCode.STEP_NOT_FOUND));
    }
}

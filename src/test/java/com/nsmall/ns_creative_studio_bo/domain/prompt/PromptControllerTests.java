package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.domain.prompt.controller.FusionImageControllerTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.controller.HumanPromptAttrControllerTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.controller.HumanPromptControllerTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.controller.HumanPromptOptionControllerTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.controller.PromptControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Prompt 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("Prompt 컨트롤러 전체 테스트")
@SelectClasses({
        PromptControllerTest.class,
        HumanPromptControllerTest.class,
        HumanPromptAttrControllerTest.class,
        HumanPromptOptionControllerTest.class,
        FusionImageControllerTest.class
})
public class PromptControllerTests {
}

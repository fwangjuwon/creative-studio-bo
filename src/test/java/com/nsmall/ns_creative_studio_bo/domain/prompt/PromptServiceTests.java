package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptAttrServiceTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptOptionServiceTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptServiceTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.HumanPromptStepServiceTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.service.PromptServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Prompt 서비스 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Suite
@SuiteDisplayName("Prompt 서비스 전체 테스트")
@SelectClasses({
        PromptServiceTest.class,
        HumanPromptServiceTest.class,
        HumanPromptStepServiceTest.class,
        HumanPromptAttrServiceTest.class,
        HumanPromptOptionServiceTest.class
})
public class PromptServiceTests {
}

package com.nsmall.ns_creative_studio_bo.domain.prompt;

import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtilTest;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.PromptHelperTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Prompt 도메인 단위 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-05-29
 */
@Suite
@SuiteDisplayName("Prompt 단위 테스트")
@SelectClasses({
        PromptHelperTest.class,
        ImageValidationUtilTest.class
})
public class PromptUnitTests {
}

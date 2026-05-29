package com.nsmall.ns_creative_studio_bo;

import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptUnitTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 전체 단위 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-05-29
 */
@Suite
@SuiteDisplayName("전체 단위 테스트")
@SelectClasses({
        PromptUnitTests.class
})
public class AllUnitTests {
}

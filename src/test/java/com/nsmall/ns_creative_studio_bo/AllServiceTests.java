package com.nsmall.ns_creative_studio_bo;

import com.nsmall.ns_creative_studio_bo.domain.user.UserServiceTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 전체 서비스 레이어 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Suite
@SuiteDisplayName("전체 서비스 테스트")
@SelectClasses({
        UserServiceTests.class
        // CategoryServiceTests.class  — Step 3-B-T에서 추가 예정
        // TagGroupServiceTests.class  — Step 3-C-T에서 추가 예정
        // PromptServiceTests.class    — Step 3-D-T에서 추가 예정
})
public class AllServiceTests {
}

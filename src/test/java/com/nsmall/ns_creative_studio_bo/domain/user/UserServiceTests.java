package com.nsmall.ns_creative_studio_bo.domain.user;

import com.nsmall.ns_creative_studio_bo.domain.user.service.UserServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * User 서비스 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Suite
@SuiteDisplayName("User 서비스 전체 테스트")
@SelectClasses({
        UserServiceTest.class
})
public class UserServiceTests {
}

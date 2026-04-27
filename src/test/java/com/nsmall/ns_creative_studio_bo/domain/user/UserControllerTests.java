package com.nsmall.ns_creative_studio_bo.domain.user;

import com.nsmall.ns_creative_studio_bo.domain.user.controller.UserControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * User 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("User 컨트롤러 전체 테스트")
@SelectClasses({
        UserControllerTest.class
})
public class UserControllerTests {
}

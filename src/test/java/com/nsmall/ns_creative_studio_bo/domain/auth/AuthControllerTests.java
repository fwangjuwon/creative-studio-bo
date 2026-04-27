package com.nsmall.ns_creative_studio_bo.domain.auth;

import com.nsmall.ns_creative_studio_bo.domain.auth.controller.AuthControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Auth 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("Auth 컨트롤러 전체 테스트")
@SelectClasses({
        AuthControllerTest.class
})
public class AuthControllerTests {
}

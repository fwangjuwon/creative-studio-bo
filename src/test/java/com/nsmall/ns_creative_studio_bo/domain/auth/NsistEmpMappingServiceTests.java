package com.nsmall.ns_creative_studio_bo.domain.auth;

import com.nsmall.ns_creative_studio_bo.domain.auth.service.NsistEmpMappingServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * NsistEmpMapping 서비스 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-05-13
 */
@Suite
@SuiteDisplayName("NsistEmpMapping 서비스 전체 테스트")
@SelectClasses({
        NsistEmpMappingServiceTest.class
})
public class NsistEmpMappingServiceTests {
}

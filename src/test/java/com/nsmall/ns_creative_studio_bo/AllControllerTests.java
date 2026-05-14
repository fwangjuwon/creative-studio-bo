package com.nsmall.ns_creative_studio_bo;

import com.nsmall.ns_creative_studio_bo.domain.auth.AuthControllerTests;
import com.nsmall.ns_creative_studio_bo.domain.auth.NsistEmpMappingControllerTests;
import com.nsmall.ns_creative_studio_bo.domain.category.CategoryControllerTests;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptControllerTests;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagControllerTests;
import com.nsmall.ns_creative_studio_bo.domain.user.UserControllerTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 전체 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("전체 컨트롤러 테스트")
@SelectClasses({
        AuthControllerTests.class,
        NsistEmpMappingControllerTests.class,
        CategoryControllerTests.class,
        UserControllerTests.class,
        TagControllerTests.class,
        PromptControllerTests.class
})
public class AllControllerTests {
}

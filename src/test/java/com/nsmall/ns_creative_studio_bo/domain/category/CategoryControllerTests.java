package com.nsmall.ns_creative_studio_bo.domain.category;

import com.nsmall.ns_creative_studio_bo.domain.category.controller.CategoryControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Category 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("Category 컨트롤러 전체 테스트")
@SelectClasses({
        CategoryControllerTest.class
})
public class CategoryControllerTests {
}

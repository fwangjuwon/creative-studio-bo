package com.nsmall.ns_creative_studio_bo.domain.category;

import com.nsmall.ns_creative_studio_bo.domain.category.service.CategoryServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Category 서비스 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Suite
@SuiteDisplayName("Category 서비스 전체 테스트")
@SelectClasses({
        CategoryServiceTest.class
})
public class CategoryServiceTests {
}

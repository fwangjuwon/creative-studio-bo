package com.nsmall.ns_creative_studio_bo.domain.tag;

import com.nsmall.ns_creative_studio_bo.domain.tag.controller.TagControllerTest;
import com.nsmall.ns_creative_studio_bo.domain.tag.controller.TagGroupControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Tag 컨트롤러 테스트 Suite
 *
 * @author iamjy
 * @since 2026-04-17
 */
@Suite
@SuiteDisplayName("Tag 컨트롤러 전체 테스트")
@SelectClasses({
        TagGroupControllerTest.class,
        TagControllerTest.class
})
public class TagControllerTests {
}

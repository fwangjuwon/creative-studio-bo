package com.nsmall.ns_creative_studio_bo.domain.tag;

import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagGroupServiceTest;
import com.nsmall.ns_creative_studio_bo.domain.tag.service.TagServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Tag 서비스 전체 테스트 Suite
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Suite
@SuiteDisplayName("Tag 서비스 전체 테스트")
@SelectClasses({
        TagGroupServiceTest.class,
        TagServiceTest.class
})
public class TagServiceTests {
}

package com.nsmall.ns_creative_studio_bo;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 전체 레이어 통합 테스트 Suite — CI/CD, 배포 전 검증용
 *
 * <pre>
 * 실행 속도가 느릴 수 있으므로 개발 중에는
 * 도메인별 Suite 또는 단일 테스트 클래스 실행을 권장.
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Suite
@SuiteDisplayName("전체 레이어 테스트")
@SelectClasses({
        AllServiceTests.class
        // AllControllerTests.class — 추가 예정
})
public class AllLayersTests {
}

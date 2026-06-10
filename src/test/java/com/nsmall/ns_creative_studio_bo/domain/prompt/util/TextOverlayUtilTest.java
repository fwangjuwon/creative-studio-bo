package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TextOverlayUtil 단위 테스트
 *
 * @author gaeddo
 * @since 2026-06-09
 */
public class TextOverlayUtilTest {

    // ─── 프롬프트 suffix 생성 (buildSuffix) ─────────────────────────────────

    @Test
    @DisplayName("[성공] suffix 생성 - 텍스트 입력 시 suffix에 포함")
    void 성공_suffix생성_텍스트포함() {
        String result = TextOverlayUtil.buildSuffix("SALE");

        assertThat(result).isNotNull();
        assertThat(result).contains("SALE");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 앞뒤 공백 제거 후 반영")
    void 성공_suffix생성_앞뒤공백제거() {
        String result = TextOverlayUtil.buildSuffix("  SALE  ");

        assertThat(result).contains("SALE");
        assertThat(result).doesNotContain("  SALE  ");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - null 입력 시 null 반환")
    void 성공_suffix생성_null입력() {
        assertThat(TextOverlayUtil.buildSuffix(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("[성공] suffix 생성 - 빈 문자열 및 공백 입력 시 null 반환")
    void 성공_suffix생성_빈문자열및공백(String input) {
        assertThat(TextOverlayUtil.buildSuffix(input)).isNull();
    }
}

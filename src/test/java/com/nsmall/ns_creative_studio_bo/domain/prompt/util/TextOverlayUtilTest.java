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

    @Test
    @DisplayName("[성공] suffix 생성 - 줄바꿈 포함 시 ' / ' 구분자로 변환")
    void 성공_suffix생성_줄바꿈_구분자변환() {
        String result = TextOverlayUtil.buildSuffix("업그레이드 최신상\n압력누름 만능팬");

        assertThat(result).contains("업그레이드 최신상 / 압력누름 만능팬");
        assertThat(result).contains("line break");
        assertThat(result).doesNotContain("\n");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 줄바꿈 없으면 line break 힌트 미포함")
    void 성공_suffix생성_줄바꿈없음_힌트미포함() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인");

        assertThat(result).contains("특가 30% 할인");
        assertThat(result).doesNotContain("line break");
        assertThat(result).doesNotContain(" / ");
    }

    // ─── 단서 문구 (disclaimerText) ──────────────────────────────────────────

    @Test
    @DisplayName("[성공] suffix 생성 - 단서 문구만 입력 시 fine-print 지시 포함")
    void 성공_suffix생성_단서문구만_disclaimer지시포함() {
        String result = TextOverlayUtil.buildSuffix(null, "26.03 출시, 자사 누름팬(HS-0110/23.07) 대비");

        assertThat(result).isNotNull();
        assertThat(result).contains("26.03 출시, 자사 누름팬(HS-0110/23.07) 대비");
        assertThat(result).contains("fine-print");
        assertThat(result).contains("smaller");
        assertThat(result).doesNotContain("Naturally integrate");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 메인 텍스트만 입력 시 기존 방식 유지")
    void 성공_suffix생성_메인텍스트만_기존방식() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인", (String) null);

        assertThat(result).contains("Naturally integrate");
        assertThat(result).doesNotContain("fine-print");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 메인+단서 동시 입력 시 둘 다 포함")
    void 성공_suffix생성_메인_단서_동시() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인", "주의사항 반드시 확인");

        assertThat(result).contains("Naturally integrate");
        assertThat(result).contains("특가 30% 할인");
        assertThat(result).contains("주의사항 반드시 확인");
        assertThat(result).contains("fine-print");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 둘 다 null이면 null 반환")
    void 성공_suffix생성_둘다null() {
        assertThat(TextOverlayUtil.buildSuffix(null, (String) null)).isNull();
    }

    // ─── 강조 텍스트 (**단어**) 파싱 ──────────────────────────────────────────

    @Test
    @DisplayName("[성공] 강조 파싱 - 마커 제거 후 표시 텍스트와 강조 단어 추출")
    void 성공_강조파싱_마커제거및단어추출() {
        TextOverlayUtil.EmphasisResult result =
                TextOverlayUtil.parseEmphasis("기능성 **냉감** 만능팬츠 4종");

        assertThat(result.displayText()).isEqualTo("기능성 냉감 만능팬츠 4종");
        assertThat(result.emphasizedWords()).containsExactly("냉감");
    }

    @Test
    @DisplayName("[성공] 강조 파싱 - 강조 마커 없으면 빈 리스트 반환")
    void 성공_강조파싱_마커없음() {
        TextOverlayUtil.EmphasisResult result = TextOverlayUtil.parseEmphasis("특가 30% 할인");

        assertThat(result.displayText()).isEqualTo("특가 30% 할인");
        assertThat(result.emphasizedWords()).isEmpty();
    }

    @Test
    @DisplayName("[성공] 강조 파싱 - 강조 마커 여러 개면 순서대로 모두 추출")
    void 성공_강조파싱_여러개() {
        TextOverlayUtil.EmphasisResult result =
                TextOverlayUtil.parseEmphasis("**겨울** 단 하루 **최저가**");

        assertThat(result.displayText()).isEqualTo("겨울 단 하루 최저가");
        assertThat(result.emphasizedWords()).containsExactly("겨울", "최저가");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 강조 마커 포함 시 강조 지시문 추가 및 마커 제거")
    void 성공_suffix생성_강조마커_지시문추가() {
        String result = TextOverlayUtil.buildSuffix("기능성 **냉감** 만능팬츠 4종");

        assertThat(result).contains("기능성 냉감 만능팬츠 4종");
        assertThat(result).contains("\"냉감\"");
        assertThat(result).contains("focal point");
        assertThat(result).doesNotContain("**");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 강조 마커 없으면 강조 지시문 미포함")
    void 성공_suffix생성_강조마커없음() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인");

        assertThat(result).doesNotContain("focal point");
    }

    // ─── 방송 자막 스타일 (홈쇼핑) ─────────────────────────────────────────────

    @Test
    @DisplayName("[성공] suffix 생성 - 메인 텍스트는 강조 마커 유무와 무관하게 항상 방송 자막 스타일 적용")
    void 성공_suffix생성_방송자막스타일_항상적용() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인");

        assertThat(result).contains("broadcast");
        assertThat(result).contains("outline");
    }

    @Test
    @DisplayName("[성공] suffix 생성 - 포스터형 구도 선택권 지시 포함")
    void 성공_suffix생성_포스터구도_선택권포함() {
        String result = TextOverlayUtil.buildSuffix("특가 30% 할인");

        assertThat(result).contains("poster");
    }
}

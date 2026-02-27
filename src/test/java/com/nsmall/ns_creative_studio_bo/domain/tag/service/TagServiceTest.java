package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagRespDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TagService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("local")
public class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagGroupService tagGroupService;

    private TagGroupRespDto 태그그룹_생성(String name) {
        return tagGroupService.createTagGroup(
                TagGroupCreateReqDto.builder()
                        .name(name)
                        .isMultiSelect(false)
                        .sortOrder(1)
                        .build()
        );
    }

    private TagCreateReqDto 태그_생성_DTO(Long tagGroupId, String name) {
        return TagCreateReqDto.builder()
                .tagGroupId(tagGroupId)
                .name(name)
                .sortOrder(1)
                .build();
    }

    // ============================================================
    // Tag 생성
    // ============================================================

    @ParameterizedTest
    @CsvSource({
            "남자",
            "여자"
    })
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 생성 - 정상 케이스")
    void 성공_태그_생성(String tagName) {
        TagGroupRespDto group = 태그그룹_생성("인물");

        TagRespDto result = tagService.createTag(태그_생성_DTO(group.getId(), tagName));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(tagName);
        assertThat(result.getTagGroupId()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("[예외] 태그 생성 - 존재하지 않는 TagGroup")
    void 예외_태그_생성_그룹_미존재() {
        TagCreateReqDto dto = 태그_생성_DTO(Long.MAX_VALUE, "미존재그룹태그");

        assertThatThrownBy(() -> tagService.createTag(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagErrorCode.TAG_GROUP_NOT_FOUND));
    }

    // ============================================================
    // Tag 단건 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 단건 조회 - 존재하는 태그")
    void 성공_태그_단건_조회_존재() {
        TagGroupRespDto group = 태그그룹_생성("단건조회그룹");
        TagRespDto created = tagService.createTag(태그_생성_DTO(group.getId(), "단건조회태그"));

        Optional<TagRespDto> result = tagService.getTag(created.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("단건조회태그");
        assertThat(result.get().getTagGroupId()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("[없음] 태그 단건 조회 - 미존재")
    void 없음_태그_단건_조회_미존재() {
        Optional<TagRespDto> result = tagService.getTag(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // ============================================================
    // Tag 목록 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 목록 조회 - TagGroup별 필터링")
    void 성공_태그_목록_조회_그룹별() {
        TagGroupRespDto group = 태그그룹_생성("인물그룹");
        tagService.createTag(태그_생성_DTO(group.getId(), "남자"));
        tagService.createTag(태그_생성_DTO(group.getId(), "여자"));

        List<TagRespDto> result = tagService.getTags(group.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(t -> assertThat(t.getTagGroupId()).isEqualTo(group.getId()));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 목록 조회 - 전체 (tagGroupId null)")
    void 성공_태그_목록_조회_전체() {
        TagGroupRespDto group1 = 태그그룹_생성("인물전체조회");
        TagGroupRespDto group2 = 태그그룹_생성("분위기전체조회");
        tagService.createTag(태그_생성_DTO(group1.getId(), "남자"));
        tagService.createTag(태그_생성_DTO(group2.getId(), "화사한"));

        List<TagRespDto> result = tagService.getTags(null);

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 목록 조회 - 다른 그룹 태그가 섞이지 않음")
    void 성공_태그_목록_조회_그룹_격리() {
        TagGroupRespDto groupA = 태그그룹_생성("그룹A격리");
        TagGroupRespDto groupB = 태그그룹_생성("그룹B격리");
        tagService.createTag(태그_생성_DTO(groupA.getId(), "그룹A태그"));
        tagService.createTag(태그_생성_DTO(groupB.getId(), "그룹B태그"));

        List<TagRespDto> resultA = tagService.getTags(groupA.getId());

        assertThat(resultA).allSatisfy(t -> assertThat(t.getTagGroupId()).isEqualTo(groupA.getId()));
        assertThat(resultA).noneMatch(t -> t.getTagGroupId().equals(groupB.getId()));
    }

    // ============================================================
    // Tag 수정
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 수정 - 이름 / 정렬순서 변경")
    void 성공_태그_수정() {
        TagGroupRespDto group = 태그그룹_생성("수정테스트그룹");
        TagRespDto created = tagService.createTag(태그_생성_DTO(group.getId(), "수정전태그"));

        TagUpdateReqDto updateDto = TagUpdateReqDto.builder()
                .name("수정후태그")
                .sortOrder(99)
                .build();

        TagRespDto updated = tagService.updateTag(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정후태그");
        assertThat(updated.getSortOrder()).isEqualTo(99);
        assertThat(updated.getTagGroupId()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("[예외] 태그 수정 - 미존재")
    void 예외_태그_수정_미존재() {
        TagUpdateReqDto updateDto = TagUpdateReqDto.builder().name("수정요청").build();

        assertThatThrownBy(() -> tagService.updateTag(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagErrorCode.TAG_NOT_FOUND));
    }

    // ============================================================
    // Tag 삭제
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 삭제 - 삭제 후 조회 시 미존재")
    void 성공_태그_삭제() {
        TagGroupRespDto group = 태그그룹_생성("삭제테스트그룹");
        TagRespDto created = tagService.createTag(태그_생성_DTO(group.getId(), "삭제테스트태그"));

        tagService.deleteTag(created.getId());

        Optional<TagRespDto> result = tagService.getTag(created.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[예외] 태그 삭제 - 미존재")
    void 예외_태그_삭제_미존재() {
        assertThatThrownBy(() -> tagService.deleteTag(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagErrorCode.TAG_NOT_FOUND));
    }
}

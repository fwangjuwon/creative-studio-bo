package com.nsmall.ns_creative_studio_bo.domain.tag.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.tag.TagGroupErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.request.TagGroupUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.tag.dto.response.TagGroupRespDto;
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
 * TagGroupService 통합 테스트 — 비즈니스 로직 + DB 연동 검증
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("local")
public class TagGroupServiceTest {

    @Autowired
    private TagGroupService tagGroupService;

    @Autowired
    private TagService tagService;

    private TagGroupCreateReqDto 단일선택_그룹_DTO(String name) {
        return TagGroupCreateReqDto.builder()
                .name(name)
                .isMultiSelect(false)
                .sortOrder(1000)
                .build();
    }

    private TagGroupCreateReqDto 복수선택_그룹_DTO(String name) {
        return TagGroupCreateReqDto.builder()
                .name(name)
                .isMultiSelect(true)
                .sortOrder(1001)
                .build();
    }

    // ============================================================
    // TagGroup 생성
    // ============================================================

    @ParameterizedTest
    @CsvSource({
            "인물, false",
            "색감 , true"
    })
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 생성 - 단일/복수 선택")
    void 성공_태그그룹_생성(String name, boolean isMultiSelect) {
        TagGroupCreateReqDto dto = TagGroupCreateReqDto.builder()
                .name(name)
                .isMultiSelect(isMultiSelect)
                .sortOrder(1000)
                .build();

        TagGroupRespDto result = tagGroupService.createTagGroup(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getIsMultiSelect()).isEqualTo(isMultiSelect);
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 생성 - sortOrder null 여러 개 허용")
    void 성공_태그그룹_생성_sortOrder_null_다수() {
        TagGroupCreateReqDto dto1 = TagGroupCreateReqDto.builder()
                .name("null순서그룹A").isMultiSelect(false).sortOrder(null).build();
        TagGroupCreateReqDto dto2 = TagGroupCreateReqDto.builder()
                .name("null순서그룹B").isMultiSelect(false).sortOrder(null).build();

        TagGroupRespDto r1 = tagGroupService.createTagGroup(dto1);
        TagGroupRespDto r2 = tagGroupService.createTagGroup(dto2);

        assertThat(r1.getId()).isNotNull();
        assertThat(r2.getId()).isNotNull();
    }

    // ============================================================
    // TagGroup 단건 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 단건 조회 - 존재하는 그룹")
    void 성공_태그그룹_단건_조회_존재() {
        TagGroupRespDto created = tagGroupService.createTagGroup(단일선택_그룹_DTO("단건조회테스트"));

        Optional<TagGroupRespDto> result = tagGroupService.getTagGroup(created.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("단건조회테스트");
        assertThat(result.get().getIsMultiSelect()).isFalse();
    }

    @Test
    @DisplayName("[없음] 태그 그룹 단건 조회 - 미존재")
    void 없음_태그그룹_단건_조회_미존재() {
        Optional<TagGroupRespDto> result = tagGroupService.getTagGroup(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // ============================================================
    // TagGroup 전체 목록 조회
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 전체 목록 조회")
    void 성공_태그그룹_전체_목록_조회() {
        tagGroupService.createTagGroup(단일선택_그룹_DTO("목록조회그룹A"));
        tagGroupService.createTagGroup(복수선택_그룹_DTO("목록조회그룹B"));

        List<TagGroupRespDto> result = tagGroupService.getTagGroups();

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(2);
    }

    // ============================================================
    // TagGroup 수정
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 수정 - 이름 / isMultiSelect / 정렬순서 변경")
    void 성공_태그그룹_수정() {
        TagGroupRespDto created = tagGroupService.createTagGroup(단일선택_그룹_DTO("수정전그룹"));

        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder()
                .name("수정후그룹")
                .isMultiSelect(true)
                .sortOrder(1999)
                .build();

        TagGroupRespDto updated = tagGroupService.updateTagGroup(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("수정후그룹");
        assertThat(updated.getIsMultiSelect()).isTrue();
        assertThat(updated.getSortOrder()).isEqualTo(1999);
    }

    @Test
    @DisplayName("[예외] 태그 그룹 수정 - 미존재")
    void 예외_태그그룹_수정_미존재() {
        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder().name("수정요청").build();

        assertThatThrownBy(() -> tagGroupService.updateTagGroup(Long.MAX_VALUE, updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.TAG_GROUP_NOT_FOUND));
    }

    // ============================================================
    // TagGroup sortOrder 중복 검증
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 태그 그룹 생성 - sortOrder 중복")
    void 예외_태그그룹_생성_sortOrder_중복() {
        tagGroupService.createTagGroup(단일선택_그룹_DTO("그룹A")); // sortOrder=1000

        TagGroupCreateReqDto dto = TagGroupCreateReqDto.builder()
                .name("그룹B")
                .isMultiSelect(false)
                .sortOrder(1000)
                .build();

        assertThatThrownBy(() -> tagGroupService.createTagGroup(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.DUPLICATE_SORT_ORDER));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 태그 그룹 수정 - sortOrder 중복")
    void 예외_태그그룹_수정_sortOrder_중복() {
        tagGroupService.createTagGroup(단일선택_그룹_DTO("그룹A")); // sortOrder=1000
        TagGroupRespDto groupB = tagGroupService.createTagGroup(복수선택_그룹_DTO("그룹B")); // sortOrder=1001

        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder()
                .name("그룹B수정")
                .isMultiSelect(false)
                .sortOrder(1000) // 그룹A와 중복
                .build();

        assertThatThrownBy(() -> tagGroupService.updateTagGroup(groupB.getId(), updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.DUPLICATE_SORT_ORDER));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 수정 - 자신의 sortOrder로 수정 (중복 허용)")
    void 성공_태그그룹_수정_자신의_sortOrder() {
        TagGroupRespDto created = tagGroupService.createTagGroup(단일선택_그룹_DTO("그룹A")); // sortOrder=1000

        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder()
                .name("그룹A수정")
                .isMultiSelect(false)
                .sortOrder(1000)
                .build();

        TagGroupRespDto updated = tagGroupService.updateTagGroup(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("그룹A수정");
        assertThat(updated.getSortOrder()).isEqualTo(1000);
    }

    // ============================================================
    // TagGroup name 중복 검증
    // ============================================================

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 태그 그룹 생성 - name 중복 (전역 스코프)")
    void 예외_태그그룹_생성_name_중복() {
        tagGroupService.createTagGroup(단일선택_그룹_DTO("중복그룹명")); // sortOrder=1000

        TagGroupCreateReqDto dto = TagGroupCreateReqDto.builder()
                .name("중복그룹명")
                .isMultiSelect(false)
                .sortOrder(1001) // sortOrder는 다르게
                .build();

        assertThatThrownBy(() -> tagGroupService.createTagGroup(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.DUPLICATE_TAG_GROUP_NAME));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 태그 그룹 수정 - name 중복")
    void 예외_태그그룹_수정_name_중복() {
        tagGroupService.createTagGroup(단일선택_그룹_DTO("그룹A")); // sortOrder=1000
        TagGroupRespDto groupB = tagGroupService.createTagGroup(복수선택_그룹_DTO("그룹B")); // sortOrder=1001

        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder()
                .name("그룹A") // 그룹A와 name 중복
                .isMultiSelect(false)
                .sortOrder(1001)
                .build();

        assertThatThrownBy(() -> tagGroupService.updateTagGroup(groupB.getId(), updateDto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.DUPLICATE_TAG_GROUP_NAME));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 수정 - 자신의 name으로 수정 (중복 허용)")
    void 성공_태그그룹_수정_자신의_name() {
        TagGroupRespDto created = tagGroupService.createTagGroup(단일선택_그룹_DTO("그룹A")); // sortOrder=1000

        TagGroupUpdateReqDto updateDto = TagGroupUpdateReqDto.builder()
                .name("그룹A") // 자신의 name
                .isMultiSelect(false)
                .sortOrder(1000)
                .build();

        TagGroupRespDto updated = tagGroupService.updateTagGroup(created.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("그룹A");
    }

    // ============================================================
    // TagGroup 삭제
    // ============================================================

    @Test
    @DisplayName("[예외] 태그 그룹 삭제 - 미존재")
    void 예외_태그그룹_삭제_미존재() {
        assertThatThrownBy(() -> tagGroupService.deleteTagGroup(Long.MAX_VALUE))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.TAG_GROUP_NOT_FOUND));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[예외] 태그 그룹 삭제 - 소속 태그 존재 시 삭제 거부")
    void 예외_태그그룹_삭제_소속태그_존재() {
        TagGroupRespDto group = tagGroupService.createTagGroup(단일선택_그룹_DTO("삭제불가그룹"));
        tagService.createTag(TagCreateReqDto.builder()
                .tagGroupId(group.getId())
                .name("소속태그")
                .sortOrder(1)
                .build());

        assertThatThrownBy(() -> tagGroupService.deleteTagGroup(group.getId()))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(TagGroupErrorCode.TAG_GROUP_HAS_TAGS));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("[성공] 태그 그룹 삭제 - 소속 태그 없을 때 삭제 허용")
    void 성공_태그그룹_삭제_소속태그_없음() {
        TagGroupRespDto group = tagGroupService.createTagGroup(단일선택_그룹_DTO("삭제가능그룹"));

        tagGroupService.deleteTagGroup(group.getId());

        assertThat(tagGroupService.getTagGroup(group.getId())).isEmpty();
    }
}

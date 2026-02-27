package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.prompt.entity.PromptHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프롬프트 히스토리 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistoryRespDto {

    private Long id;
    private Long userId;
    private Long categoryId;
    private String promptText;
    private List<Long> tagIds;
    private String initRegiPic;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    public static PromptHistoryRespDto from(PromptHistory history, List<Long> tagIds) {
        return PromptHistoryRespDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .categoryId(history.getCategoryId())
                .promptText(history.getPromptText())
                .tagIds(tagIds)
                .initRegiPic(history.getInitRegiPic())
                .initRegiDttm(history.getInitRegiDttm())
                .finalRvisDttm(history.getFinalRvisDttm())
                .build();
    }
}

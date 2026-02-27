package com.nsmall.ns_creative_studio_bo.domain.prompt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 프롬프트 생성 요청 DTO
 *
 * <pre>
 * categoryId: 최종 선택한 DCS 카테고리 ID
 * tagIds: 선택한 태그 ID 목록 (빈 리스트 허용)
 *
 * 요청 사용자 ID는 JWT SecurityContext에서 추출 — 클라이언트가 전달하지 않음
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptCreateReqDto {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @Builder.Default
    private List<Long> tagIds = List.of();
}

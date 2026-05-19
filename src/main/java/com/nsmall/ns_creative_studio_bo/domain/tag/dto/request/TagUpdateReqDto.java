package com.nsmall.ns_creative_studio_bo.domain.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tag 수정 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagUpdateReqDto {

    @NotBlank(message = "태그명은 필수입니다.")
    @Size(max = 100, message = "태그명은 100자 이하여야 합니다.")
    private String name;

    /** 실제 사용값 (선택). null이면 name을 그대로 사용. */
    @Size(max = 50, message = "태그 값은 50자 이하여야 합니다.")
    private String tagValue;

    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;
}

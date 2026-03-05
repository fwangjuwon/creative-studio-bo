package com.nsmall.ns_creative_studio_bo.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User 검색 요청 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchReqDto {

    private String name;
    private String teamName;
    private String role;
}

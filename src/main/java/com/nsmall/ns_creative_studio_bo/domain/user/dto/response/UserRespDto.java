package com.nsmall.ns_creative_studio_bo.domain.user.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User 응답 DTO
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRespDto {

    private Long id;
    private String loginId;
    private String name;
    private String role;
    private String teamName;
    private Integer dailyLimit;
    private LocalDateTime initRegiDttm;
    private LocalDateTime finalRvisDttm;

    public static UserRespDto from(User user) {
        return UserRespDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .role(user.getRole())
                .teamName(user.getTeamName())
                .dailyLimit(user.getDailyLimit())
                .initRegiDttm(user.getInitRegiDttm())
                .finalRvisDttm(user.getFinalRvisDttm())
                .build();
    }
}

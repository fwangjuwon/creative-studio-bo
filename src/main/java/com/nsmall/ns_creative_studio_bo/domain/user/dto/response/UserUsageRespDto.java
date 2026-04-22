package com.nsmall.ns_creative_studio_bo.domain.user.dto.response;

import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 일일 사용량 응답 DTO
 *
 * <pre>
 * dailyLimit      : 해당 사용자에게 할당된 일일 프롬프트 생성 횟수
 * todayUsedCount  : 오늘(CURRENT_DATE) 실제 사용 횟수
 * remainingCount  : 남은 횟수 (dailyLimit - todayUsedCount, 음수 방지)
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-03-12
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUsageRespDto {

    private Long   userId;
    private String loginId;
    private String name;
    private int    dailyLimit;
    private String useYn;
    private int    todayUsedCount;
    private int    remainingCount;

    /**
     * User 엔티티와 오늘 사용 횟수로 응답 DTO 생성.
     * 단일 엔티티 변환이 아닌 두 값의 조합이므로 {@code of()} 네이밍 사용.
     */
    public static UserUsageRespDto of(User user, int todayUsedCount) {
        return UserUsageRespDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .dailyLimit(user.getDailyLimit())
                .useYn(user.getUseYn())
                .todayUsedCount(todayUsedCount)
                .remainingCount(Math.max(0, user.getDailyLimit() - todayUsedCount))
                .build();
    }
}

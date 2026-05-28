package com.nsmall.ns_creative_studio_bo.domain.prompt.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.PromptErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 공통 유틸리티 컴포넌트
 *
 * <pre>
 * PromptServiceImpl, HumanPromptServiceImpl, FusionImageServiceImpl 세 서비스에서
 * 공통으로 사용되는 로직을 모아 중복을 제거한다.
 *
 * - checkDailyLimit(userId)          : 사용자 조회 + 일일 한도 초과 여부 검증
 * - toAbsoluteUrls(paths)            : 상대 경로 목록 → 절대 URL 목록 변환
 * - serializeImageUrls(imageUrls)    : 이미지 URL 목록 → JSON 문자열 직렬화 (실패 시 첫 번째 URL 반환)
 * </pre>
 *
 * @author gaeddo
 * @since 2026-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptHelper {

    private final UserMapper userMapper;
    private final PromptHistoryMapper promptHistoryMapper;
    private final ObjectMapper objectMapper;

    @Value("${image.base-url}")
    private String imageBaseUrl;

    /**
     * 사용자 조회 후 일일 한도 초과 여부를 검증한다.
     *
     * @param userId 검증 대상 사용자 ID
     * @throws BizException 사용자 없음 또는 일일 한도 초과 시
     */
    public void checkDailyLimit(Long userId) {
        User user = userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(PromptErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + userId));

        int todayCount = promptHistoryMapper.countTodayByUserId(userId);
        if (todayCount >= user.getDailyLimit()) {
            log.warn("일일 한도 초과 — userId={}, todayCount={}, dailyLimit={}",
                    userId, todayCount, user.getDailyLimit());
            throw new BizException(PromptErrorCode.DAILY_LIMIT_EXCEEDED);
        }
    }

    /**
     * 상대 경로 목록을 절대 URL 목록으로 변환한다.
     *
     * <pre>
     * "/images/uuid.jpg" → "http://host/images/uuid.jpg"
     * </pre>
     *
     * @param relativePaths 상대 경로 목록 (예: ["/images/uuid.jpg"])
     * @return 절대 URL 목록
     */
    public List<String> toAbsoluteUrls(List<String> relativePaths) {
        return relativePaths.stream()
                .map(path -> imageBaseUrl + path)
                .toList();
    }

    /**
     * 이미지 URL 목록을 JSON 문자열로 직렬화한다.
     *
     * <pre>
     * 직렬화 실패 시 첫 번째 URL을 단독 문자열로 반환한다.
     * </pre>
     *
     * @param imageUrls 직렬화할 이미지 URL 목록
     * @return JSON 문자열 (예: "["/images/a.jpg","/images/b.jpg"]") 또는 첫 번째 URL, 빈/null이면 "[]"
     */
    public String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            log.warn("이미지 URL JSON 직렬화 실패 — 첫 번째 URL로 대체", e);
            return imageUrls.get(0);
        }
    }

    /**
     * DB에 저장된 imageUrl 문자열을 상대 경로 목록으로 파싱한다.
     *
     * <pre>
     * JSON 배열 형태("[/images/a.jpg,...]")이면 역직렬화, 단일 문자열이면 그대로 반환.
     * 파싱 실패 시 원본 문자열을 단일 원소 목록으로 반환한다.
     * </pre>
     *
     * @param imageUrl DB의 image_url 컬럼 값
     * @return 상대 경로 목록
     */
    public List<String> parseImageUrls(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return List.of();
        }
        if (imageUrl.startsWith("[")) {
            try {
                return objectMapper.readValue(imageUrl,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                log.warn("이미지 URL JSON 파싱 실패 — 단일 경로로 처리. imageUrl={}", imageUrl, e);
            }
        }
        return List.of(imageUrl);
    }
}

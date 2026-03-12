package com.nsmall.ns_creative_studio_bo.domain.user.service;

import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserUsageRespDto;

import java.util.List;
import java.util.Optional;

/**
 * User 서비스 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
public interface UserService {

    UserRespDto createUser(UserCreateReqDto dto);

    /**
     * 단건 조회 — 결과 없으면 Optional.empty() 반환 (컨트롤러에서 200 + data:null 처리)
     */
    Optional<UserRespDto> getUser(Long id);

    List<UserRespDto> getUsers(UserSearchReqDto dto);

    UserRespDto updateUser(Long id, UserUpdateReqDto dto);

    void deleteUser(Long id);

    /**
     * 사용자 일일 사용량 조회 — 본인 및 ADMIN의 특정 유저 조회 모두 이 메서드 사용
     */
    UserUsageRespDto getUserUsage(Long userId);

    /**
     * 전체 회원의 금일 사용량 목록 조회 — ADMIN 전용
     * DB JOIN 단일 쿼리로 처리 (N+1 없음)
     */
    List<UserUsageRespDto> getUserUsageList();
}

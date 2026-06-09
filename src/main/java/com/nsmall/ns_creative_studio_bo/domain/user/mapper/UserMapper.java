package com.nsmall.ns_creative_studio_bo.domain.user.mapper;

import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserUsageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * User MyBatis Mapper 인터페이스
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Mapper
public interface UserMapper {

    void insert(User user);

    Optional<User> selectById(@Param("id") Long id);

    Optional<User> selectByLoginId(@Param("loginId") String loginId);

    List<User> selectList(UserSearchReqDto dto);

    void update(User user);

    void updatePassword(@Param("id") Long id, @Param("password") String password,
                        @Param("finalRvisPic") String finalRvisPic);

    void deleteById(@Param("id") Long id);

    List<UserUsageRespDto> selectListWithTodayUsage();
}

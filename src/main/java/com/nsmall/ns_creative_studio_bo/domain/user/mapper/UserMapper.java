package com.nsmall.ns_creative_studio_bo.domain.user.mapper;

import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

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

    Optional<User> selectById(Long id);

    Optional<User> selectByLoginId(String loginId);

    List<User> selectList(UserSearchReqDto dto);

    void update(User user);

    void deleteById(Long id);
}

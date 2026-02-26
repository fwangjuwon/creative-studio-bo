package com.nsmall.ns_creative_studio_bo.domain.user.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.user.UserErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User 서비스 구현체
 *
 * @author jjhoo8936
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserRespDto createUser(UserCreateReqDto dto) {
        userMapper.selectByLoginId(dto.getLoginId()).ifPresent(u -> {
            throw new BizException(UserErrorCode.DUPLICATE_LOGIN_ID);
        });

        LocalDateTime now = LocalDateTime.now();
        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        User user = User.builder()
                .loginId(dto.getLoginId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .role(dto.getRole())
                .teamName(dto.getTeamName())
                .dailyLimit(dto.getDailyLimit())
                .initRegiPic(requesterId)
                .initRegiDttm(now)
                .finalRvisPic(requesterId)
                .finalRvisDttm(now)
                .build();

        userMapper.insert(user);

        return userMapper.selectById(user.getId())
                .map(UserRespDto::from)
                .orElse(null);
    }

    @Override
    public Optional<UserRespDto> getUser(Long id) {
        return userMapper.selectById(id).map(UserRespDto::from);
    }

    @Override
    public List<UserRespDto> getUsers(UserSearchReqDto dto) {
        return userMapper.selectList(dto).stream()
                .map(UserRespDto::from)
                .toList();
    }

    @Override
    public UserRespDto updateUser(Long id, UserUpdateReqDto dto) {
        User existing = userMapper.selectById(id)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + id));

        // TODO: Step 4 (Security 추가) 이후 SecurityContext에서 현재 로그인 사용자 loginId로 교체
        String requesterId = "system";

        String encodedPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? passwordEncoder.encode(dto.getPassword())
                : null;

        User updated = User.builder()
                .id(existing.getId())
                .password(encodedPassword)
                .name(dto.getName())
                .role(dto.getRole())
                .teamName(dto.getTeamName())
                .dailyLimit(dto.getDailyLimit())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        userMapper.update(updated);

        return userMapper.selectById(id)
                .map(UserRespDto::from)
                .orElse(null);
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.selectById(id)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + id));

        userMapper.deleteById(id);
    }
}

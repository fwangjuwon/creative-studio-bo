package com.nsmall.ns_creative_studio_bo.domain.user.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.common.util.SecurityUtil;
import com.nsmall.ns_creative_studio_bo.domain.prompt.mapper.PromptHistoryMapper;
import com.nsmall.ns_creative_studio_bo.domain.user.UserErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserCreateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserSearchReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.request.UserUpdateReqDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.dto.response.UserUsageRespDto;
import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import com.nsmall.ns_creative_studio_bo.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PromptHistoryMapper promptHistoryMapper;

    @Override
    @Transactional
    public UserRespDto createUser(UserCreateReqDto dto) {
        userMapper.selectByLoginId(dto.getLoginId()).ifPresent(u -> {
            throw new BizException(UserErrorCode.DUPLICATE_LOGIN_ID);
        });

        LocalDateTime now = LocalDateTime.now();
        String requesterId = SecurityUtil.getCurrentLoginId();

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
    @Transactional
    public UserRespDto updateUser(Long id, UserUpdateReqDto dto) {
        User existing = userMapper.selectById(id)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + id));

        String requesterId = SecurityUtil.getCurrentLoginId();

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
                .useYn(dto.getUseYn())
                .finalRvisPic(requesterId)
                .finalRvisDttm(LocalDateTime.now())
                .build();

        userMapper.update(updated);

        return userMapper.selectById(id)
                .map(UserRespDto::from)
                .orElse(null);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword), SecurityUtil.getCurrentLoginId());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.selectById(id)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + id));

        userMapper.deleteById(id);
    }

    @Override
    public UserUsageRespDto getUserUsage(Long userId) {
        User user = userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + userId));
        int todayUsedCount = promptHistoryMapper.countTodayByUserId(userId);
        return UserUsageRespDto.of(user, todayUsedCount);
    }

    @Override
    public List<UserUsageRespDto> getUserUsageList() {
        return userMapper.selectListWithTodayUsage();
    }
}

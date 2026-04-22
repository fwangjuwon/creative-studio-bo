package com.nsmall.ns_creative_studio_bo.common.security;

import com.nsmall.ns_creative_studio_bo.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security {@link UserDetails} 구현체 — DB {@link User} 엔티티 래핑
 *
 * <pre>
 * Spring Security 인증 컨텍스트에서 사용자 정보를 담는 객체.
 * getAuthorities()는 User.role 값에 "ROLE_" 접두사를 붙여 반환한다.
 * (예: "ADMIN" → "ROLE_ADMIN")
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /** DB User.id */
    public Long getId() {
        return user.getId();
    }

    /** DB User.role (예: "ADMIN", "MANAGER", "USER") */
    public String getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** loginId를 username으로 사용 */
    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Y".equals(user.getUseYn());
    }
}

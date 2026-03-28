package com.group4.javagrader.security;

import com.group4.javagrader.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final List<GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(
            Long id,
            String username,
            String password,
            String fullName,
            List<GrantedAuthority> authorities,
            boolean enabled
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public static CustomUserDetails fromUser(User user) {
        String normalizedRole = user.getRole().startsWith("ROLE_")
                ? user.getRole()
                : "ROLE_" + user.getRole();

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                List.of(new SimpleGrantedAuthority(normalizedRole)),
                user.isEnabled()
        );
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
        return enabled;
    }
}

package com.rashad.Video_Streaming_Backend.config;

import com.rashad.Video_Streaming_Backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(UserPrincipal.class);

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
        logger.info("UserPrincipal created for user with email: {}", user.getEmail());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        logger.info("Retrieving authorities for user: {}", user.getEmail());
        if (user.getRole() == null || user.getRole().name()==null) {
            logger.error("User role is not defined for user: {}", user.getEmail());
//            throw new RuntimeException("User role is not defined.");
            return List.of();
        }
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());
        logger.info("Assigned authority: {}", authority.getAuthority());
        return List.of(authority);
    }

    @Override
    public String getPassword() {
        logger.debug("Retrieving password for user: {}", user.getEmail());
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        logger.debug("Retrieving username (email) for user: {}", user.getEmail());
        return user.getEmail(); // Using email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        logger.debug("Checking if account is non-expired for user: {}", user.getEmail());
        return true; // Customize based on your requirements
    }

    @Override
    public boolean isAccountNonLocked() {
        logger.debug("Checking if account is non-locked for user: {}", user.getEmail());
        return true; // Customize based on your requirements
    }

    @Override
    public boolean isCredentialsNonExpired() {
        logger.debug("Checking if credentials are non-expired for user: {}", user.getEmail());
        return true; // Customize based on your requirements
    }

    @Override
    public boolean isEnabled() {
        logger.debug("Checking if account is enabled for user: {}", user.getEmail());
        return user.isEnabled();
    }
}

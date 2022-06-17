package com.example.javadoc0032022.security.service;

import com.example.javadoc0032022.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Data
public class UserDetailsImpl implements UserDetails {

    private int id;
    private String login;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private String name;
    private String lastName;
    private String surName;
    private String email;
    private String phoneNumber;
    private int countDocuments;
    private boolean existEcp;

    private boolean isTimeLocked;
    private boolean isPasswordExpired;
    private boolean isNonBlocked;
    private int loginAttempts;
    private long blockTime;
    private boolean enabled;

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRole().name()))
                .collect(Collectors.toList());
        return new UserDetailsImpl(user.getId(), user.getLogin(), user.getPassword(), authorities, user.getName(),
                user.getLastName(), user.getSurName(), user.getEmail(), user.getPhoneNumber(), user.getCountDocuments(),
                user.isExistEcp(), user.isTimeLocked(), user.isPasswordExpired(), user.isNonBlocked(), user.getLoginAttempts(),
                user.getBlockTime(), user.isEnabled());
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isNonBlocked;
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

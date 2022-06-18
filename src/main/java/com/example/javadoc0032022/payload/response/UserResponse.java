package com.example.javadoc0032022.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserResponse {

    private int id;
    private String login;

    @JsonIgnore
    private String password;

    private Set<String> roles = new HashSet<>();

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
    private boolean firstLogin;


}

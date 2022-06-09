package com.example.javadoc0032022.payload.response;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Data
public class JwtResponse {

    private int id;
    private String token;
    private String login;
    private List<String> roles;
    private String name;
    private String lastName;
    private String surName;
    private String email;
    private String phoneNumber;
    private boolean isNonBlocked;

    public JwtResponse(int id, String token, String login, List<String> roles, String name, String lastName, String surName, String email, String phoneNumber, boolean isNonBlocked) {
        this.id = id;
        this.token = token;
        this.login = login;
        this.roles = roles;
        this.name = name;
        this.lastName = lastName;
        this.surName = surName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isNonBlocked = isNonBlocked;
    }
}

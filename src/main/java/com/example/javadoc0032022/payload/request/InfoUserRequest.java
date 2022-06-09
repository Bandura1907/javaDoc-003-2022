package com.example.javadoc0032022.payload.request;

import lombok.Data;

import java.util.Set;

@Data
public class InfoUserRequest {
    private String login;
    private Set<String> roles;
    private String name;
    private String lastName;
    private String surName;
    private String email;
    private String phoneNumber;
}

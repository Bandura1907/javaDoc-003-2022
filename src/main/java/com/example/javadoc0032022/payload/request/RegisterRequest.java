package com.example.javadoc0032022.payload.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
public class RegisterRequest {
    private String login;

    @Size(min = 12)
    private String password;
    private Set<String> role;
    private String name;
    private String lastName;
    private String surName;

    @Email
    private String email;
    private String phoneNumber;
}

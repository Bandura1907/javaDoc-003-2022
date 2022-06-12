package com.example.javadoc0032022.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
public class RegisterRequest {

    @Schema(description = "Логин", example = "ivan122321")
    private String login;

    @Schema(description = "Пароль", example = "password13242")
    @Size(min = 12)
    private String password;

    @Schema(description = "Роль", example = "[\"admin\"...]")
    private Set<String> role;

    @Schema(description = "Имя", example = "Ivan")
    private String name;

    @Schema(description = "Фамилия", example = "IVanov")
    private String lastName;

    @Schema(description = "Очество", example = "Ivanovich")
    private String surName;

    @Email
    @Schema(description = "Email", example = "ivan@gmail.com")
    private String email;

    @Schema(description = "Телефон", example = "+380995785755")
    private String phoneNumber;
}

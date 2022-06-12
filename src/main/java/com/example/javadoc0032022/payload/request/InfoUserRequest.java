package com.example.javadoc0032022.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import java.util.Set;

@Data
public class InfoUserRequest {

    @Schema(description = "Логин")
    private String login;

    @Schema(description = "Роли", example = "[\"admin\", \"employee\", \"user\"]")
    private Set<String> roles;

    @Schema(description = "Имя юзера", example = "Ivan")
    private String name;

    @Schema(description = "Фамилия", example = "Ivanov")
    private String lastName;

    @Schema(description = "Очество", example = "Ivanovich")
    private String surName;

    @Schema(description = "Email", example = "ivan@gmail.com")
    @Email
    private String email;

    @Schema(description = "телефон", example = "+380994354323")
    private String phoneNumber;
}

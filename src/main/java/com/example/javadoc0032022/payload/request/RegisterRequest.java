package com.example.javadoc0032022.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Data
public class RegisterRequest {

    @Schema(description = "Логин", example = "ivan122321")
    @NotEmpty
    private String login;

    @Schema(description = "Пароль", example = "password13242")
    @NotEmpty
//    @Pattern(regexp = "^((?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$&*])(?=.*[0-9])){4,12}$", message = "max length 12")
    private String password;

    @Schema(description = "Роль", example = "['admin']")
    private Set<String> role;

    @Schema(description = "Имя", example = "Ivan")
    @NotEmpty
    private String name;

    @Schema(description = "Фамилия", example = "Ivanov")
    @NotEmpty
    private String lastName;

    @Schema(description = "Очество", example = "Ivanovich")
    @NotEmpty
    private String surName;

    @Email
    @NotEmpty
    @Schema(description = "Email", example = "ivan@gmail.com")
    private String email;

    @Schema(description = "Телефон", example = "+380995785755")
    private String phoneNumber;

    private String nameOrganization;
    private String mainStateRegistrationNumber;
    private String identificationNumber;
    private String position;
    private String subdivision;
}

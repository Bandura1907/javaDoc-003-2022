package com.example.javadoc0032022.payload.request;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ChangePasswordRequest {

    private String oldPassword;

    @Size(min = 12)
    private String newPassword;
}

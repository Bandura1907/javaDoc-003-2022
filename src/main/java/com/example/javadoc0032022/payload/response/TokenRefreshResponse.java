package com.example.javadoc0032022.payload.response;

import lombok.Data;

@Data
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;

    private int userId;

    public TokenRefreshResponse(String accessToken, String refreshToken, int id) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        userId = id;
    }
}

package com.example.monsterhunter.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/refresh 和 /api/auth/logout 共用的請求格式：帶上目前手上的 refreshToken 字串。 */
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

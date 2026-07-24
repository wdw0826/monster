package com.example.monsterhunter.dto;

import com.example.monsterhunter.security.UserPrincipal;

import java.util.List;

/**
 * 登入／換發 token 成功後回傳給前端的格式：JWT 本身（accessToken/refreshToken）、
 * token 類型（固定是 "Bearer"，前端組 Authorization header 時要用），以及使用者基本資訊。
 */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, String tokenType,
                         Long userId, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.roles = roles;
    }

    public static AuthResponse of(String accessToken, String refreshToken, UserPrincipal principal) {
        return new AuthResponse(
            accessToken, refreshToken, "Bearer",
            principal.getId(), principal.getUsername(), principal.getRoleNames()
        );
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}

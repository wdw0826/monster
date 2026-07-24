package com.example.monsterhunter.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/login 的請求格式：帳號 + 密碼，兩個都不能空白。 */
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

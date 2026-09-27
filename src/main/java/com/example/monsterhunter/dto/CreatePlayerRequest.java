package com.example.monsterhunter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/players/me 的請求格式：建立自己的獵人角色。
 * name 是獵人在遊戲裡的顯示名稱，跟登入帳號的 username 是分開的兩件事，可以取不一樣。
 */
public class CreatePlayerRequest {

    @NotBlank
    @Size(max = 50, message = "名稱最多 50 個字")
    private String name;

    public CreatePlayerRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

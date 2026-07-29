package com.example.monsterhunter.dto;

import jakarta.validation.constraints.NotBlank;

/** PATCH /api/players/me 的請求格式：改自己獵人的顯示名稱。 */
public class UpdatePlayerRequest {

    @NotBlank
    private String name;

    public UpdatePlayerRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

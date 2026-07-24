package com.example.monsterhunter.dto;

import com.example.monsterhunter.entity.Weapon;

/**
 * 回傳給前端的武器資料格式，通常掛在 PlayerResponse 裡一起回傳。
 * 建構子接受 null（角色還沒裝備武器的情況），這時各欄位就保持預設值。
 */
public class WeaponResponse {
    private Long id;
    private String name;
    private int attackBonus;

    public WeaponResponse() {
    }

    public WeaponResponse(Weapon weapon) {
        if (weapon == null) {
            return;
        }
        this.id = weapon.getId();
        this.name = weapon.getName();
        this.attackBonus = weapon.getAttackBonus();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAttackBonus() {
        return attackBonus;
    }
}

package com.example.monsterhunter.dto;

import com.example.monsterhunter.entity.Player;

/**
 * 回傳給前端的獵人角色資料格式，把 Player entity（含裝備的武器）整理成 API 回應用的欄位，
 * 同時算出 totalAttack（基礎攻擊 + 武器加成）方便前端直接顯示，不用自己再算一次。
 */
public class PlayerResponse {
    private Long id;
    private String name;
    private int level;
    private int exp;
    private int hp;
    private int maxHp;
    private int baseAttack;
    private int totalAttack;
    private int money;
    private int smallPotions;
    private int bigPotions;
    private WeaponResponse weapon;

    public PlayerResponse() {
    }

    public PlayerResponse(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.hp = player.getHp();
        this.maxHp = player.getMaxHp();
        this.baseAttack = player.getAttack();
        this.totalAttack = player.getTotalAttack();
        this.money = player.getMoney();
        this.smallPotions = player.getSmallPotions();
        this.bigPotions = player.getBigPotions();
        this.weapon = new WeaponResponse(player.getWeapon());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getTotalAttack() {
        return totalAttack;
    }

    public int getMoney() {
        return money;
    }

    public int getSmallPotions() {
        return smallPotions;
    }

    public int getBigPotions() {
        return bigPotions;
    }

    public WeaponResponse getWeapon() {
        return weapon;
    }
}

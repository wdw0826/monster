package com.example.monsterhunter.dto;

import com.example.monsterhunter.entity.Monster;

/**
 * 回傳給前端的魔物資料格式，把 Monster entity 轉換成 API 該回傳的欄位。
 * 不直接把 entity 序列化回傳，是為了跟資料庫結構脫鉤——以後 entity 加欄位，
 * API 回應格式不會跟著意外變動。
 */
public class MonsterResponse {
    private Long id;
    private String name;
    private int hp;
    private int maxHp;
    private int attack;
    private int expValue;

    public MonsterResponse() {
    }

    public MonsterResponse(Monster monster) {
        this.id = monster.getId();
        this.name = monster.getName();
        this.hp = monster.getHp();
        this.maxHp = monster.getMaxHp();
        this.attack = monster.getAttack();
        this.expValue = monster.getExpValue();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getExpValue() {
        return expValue;
    }
}

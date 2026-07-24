package com.example.monsterhunter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 武器，一把只會掛在一個 Player 身上（一對一關係）。新建獵人時會自動配一把「初階獵刀」，
 * 在商店強化武器時不是修改原本這把的數值，而是整把換成新的一把（見 StoreService.upgradeWeapon），
 * 名稱會疊加 (+1)/(+2)/... 標記強化了幾次。
 */
@Entity
@Table(name = "weapons")
public class Weapon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int attackBonus;

    protected Weapon() {
    }

    public Weapon(String name, int attackBonus) {
        this.name = name;
        this.attackBonus = attackBonus;
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

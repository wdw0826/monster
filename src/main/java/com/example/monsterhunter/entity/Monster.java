package com.example.monsterhunter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 魔物。每個 Quest 掛一隻，代表這個任務要打的敵人。
 * hp 歸零代表被打贏了；respawn() 會把血量補回 maxHp，
 * 讓這個任務下一次被接的時候，魔物又是滿血狀態重新開打。
 */
@Entity
@Table(name = "monsters")
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int hp;

    private int maxHp;

    private int attack;

    private int expValue;

    protected Monster() {
    }

    public Monster(String name, int hp, int attack, int expValue) {
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.attack = attack;
        this.expValue = expValue;
    }

    public void takeDamage(int dmg) {
        this.hp -= dmg;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }

    public void respawn() {
        this.hp = this.maxHp;
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

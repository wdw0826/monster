package com.example.monsterhunter.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 玩家的獵人角色，是整個遊戲玩法的主體。一個登入帳號（User）只能有一隻獵人
 * （players.user_id 有唯一約束），登入後呼叫 POST /api/players/me 建立。
 * 之後戰鬥、接任務、逛商店等所有遊戲 API，都是靠 JWT 解出 userId 找到「自己的獵人」來操作，
 * 呼叫端不會、也不能指定要操作哪個 playerId，避免冒用別人角色。
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String name;

    private int hp = 100;

    private int maxHp = 100;

    /** 基礎攻擊力，不含武器加成。 */
    private int attack = 10;

    private int level = 1;

    private int exp = 0;

    private int money = 500;

    private int smallPotions = 0;

    private int bigPotions = 3;

    // 同 Quest.monster 的理由：預設 EAGER 會讓 GET /api/admin/players 這種列表 API
    // 產生 N+1，改 LAZY 後由 PlayerRepository.findAllWithWeapon() 的 JOIN FETCH 一次撈好。
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "weapon_id")
    private Weapon weapon;

    protected Player() {
    }

    public Player(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public int getTotalAttack() {
        return attack + (weapon != null ? weapon.getAttackBonus() : 0);
    }

    public void takeDamage(int dmg) {
        this.hp -= dmg;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }

    public void heal(int amount) {
        this.hp += amount;
        if (this.hp > maxHp) {
            this.hp = maxHp;
        }
    }

    public void equipWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    /** 每次滿 100 exp 升級一次：攻擊 +5、血量全滿、exp 歸零。 */
    public void gainExp(int amount) {
        exp += amount;
        while (exp >= 100) {
            level++;
            exp -= 100;
            attack += 5;
            hp = maxHp;
        }
    }

    public void subMoney(int amount) {
        this.money -= amount;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public void addSmallPotions(int count) {
        this.smallPotions += count;
    }

    public void subSmallPotions(int count) {
        this.smallPotions -= count;
    }

    public void addBigPotions(int count) {
        this.bigPotions += count;
    }

    public void subBigPotions(int count) {
        this.bigPotions -= count;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
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

    public Weapon getWeapon() {
        return weapon;
    }
}

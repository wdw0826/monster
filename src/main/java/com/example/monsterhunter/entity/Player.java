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
import jakarta.persistence.Version;

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
    //
    // 只 cascade 新增／合併，不 cascade 刪除、也不開 orphanRemoval：
    // 角色被刪時由資料庫 trigger 把武器一起刪（V4 migration），不管是 admin API 刪角色、
    // 還是刪帳號讓資料庫 CASCADE 連帶刪角色都一樣，不會留下孤兒武器。
    // 強化武器換下來的舊武器由 StoreService.upgradeWeapon 明確刪除。
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "weapon_id")
    private Weapon weapon;

    // 樂觀鎖：同一個玩家連點「買藥水／強化武器」時，多個請求會讀到同一份金錢，
    // 沒有 version 的話後寫的會蓋掉先寫的（回 200 但實際沒買到）。有了 version，衝突的那次會回 409。
    @Version
    private Long version;

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

    public void rename(String name) {
        this.name = name;
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

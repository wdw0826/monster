package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.PotionType;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Weapon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商店的業務邏輯：藥水購買、武器強化。兩個操作都是先檢查錢夠不夠、扣錢，
 * 再修改玩家身上的狀態；強化武器不是改原本那把的數值，而是整把換新的（見 upgradeWeapon）。
 */
@Service
public class StoreService {

    private static final int SMALL_POTION_PRICE = 100;
    private static final int BIG_POTION_PRICE = 200;
    private static final int UPGRADE_PRICE = 500;
    private static final int UPGRADE_ATTACK_BONUS = 15;

    @Transactional
    public Player buyPotion(Player player, PotionType type) {
        int price = (type == PotionType.SMALL) ? SMALL_POTION_PRICE : BIG_POTION_PRICE;
        if (player.getMoney() < price) {
            throw new IllegalStateException("金錢不足，無法購買藥水");
        }
        player.subMoney(price);
        if (type == PotionType.SMALL) {
            player.addSmallPotions(1);
        } else {
            player.addBigPotions(1);
        }
        return player;
    }

    /** 強化目前裝備的武器：整把換新的，攻擊力疊加，名稱疊加 (+1)/(+2)/... */
    @Transactional
    public Player upgradeWeapon(Player player) {
        if (player.getMoney() < UPGRADE_PRICE) {
            throw new IllegalStateException("金錢不足，無法強化武器");
        }
        player.subMoney(UPGRADE_PRICE);

        Weapon current = player.getWeapon();
        String newName;
        int newAttack;

        if (current == null) {
            newName = "初階獵刀(+1)";
            newAttack = UPGRADE_ATTACK_BONUS;
        } else {
            String oldName = current.getName();
            newAttack = current.getAttackBonus() + UPGRADE_ATTACK_BONUS;

            if (oldName.contains("(+")) {
                int startIndex = oldName.lastIndexOf("(+") + 2;
                int endIndex = oldName.lastIndexOf(")");
                try {
                    int level = Integer.parseInt(oldName.substring(startIndex, endIndex));
                    level++;
                    String baseName = oldName.substring(0, oldName.lastIndexOf("(+"));
                    newName = baseName + "(+" + level + ")";
                } catch (Exception e) {
                    newName = oldName + "(+1)";
                }
            } else {
                newName = oldName + "(+1)";
            }
        }

        player.equipWeapon(new Weapon(newName, newAttack));
        return player;
    }
}

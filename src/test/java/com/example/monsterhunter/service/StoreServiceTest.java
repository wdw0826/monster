package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.PotionType;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Weapon;
import com.example.monsterhunter.repository.PlayerRepository;
import com.example.monsterhunter.repository.WeaponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreServiceTest {

    private static final Long USER_ID = 7L;

    private StoreService storeService;
    private Player player;
    private final List<Object> deletedWeapons = new ArrayList<>();

    @BeforeEach
    void setUp() {
        player = new Player(USER_ID, "測試獵人");            // 金錢 500
        player.equipWeapon(new Weapon("初階獵刀", 5));
        PlayerRepository playerRepository = Fakes.of(PlayerRepository.class, "findByUserId",
                args -> USER_ID.equals(args[0]) ? Optional.of(player) : Optional.empty());
        WeaponRepository weaponRepository = Fakes.of(WeaponRepository.class, "delete",
                args -> deletedWeapons.add(args[0]));
        storeService = new StoreService(playerRepository, weaponRepository);
    }

    @Test
    void 買大藥水扣200() {
        storeService.buyPotion(USER_ID, PotionType.BIG);

        assertThat(player.getMoney()).isEqualTo(300);
        assertThat(player.getBigPotions()).isEqualTo(4);
    }

    @Test
    void 錢不夠不能買也不扣錢() {
        storeService.buyPotion(USER_ID, PotionType.BIG);
        storeService.buyPotion(USER_ID, PotionType.BIG);   // 剩 100

        assertThatThrownBy(() -> storeService.buyPotion(USER_ID, PotionType.BIG))
                .isInstanceOf(IllegalStateException.class);
        assertThat(player.getMoney()).isEqualTo(100);
    }

    @Test
    void 強化武器_名稱疊加_攻擊加15() {
        storeService.upgradeWeapon(USER_ID);

        assertThat(player.getWeapon().getName()).isEqualTo("初階獵刀(+1)");
        assertThat(player.getWeapon().getAttackBonus()).isEqualTo(20);
        assertThat(player.getMoney()).isEqualTo(0);
    }

    @Test
    void 強化後舊武器會被刪掉() {
        Weapon original = player.getWeapon();
        storeService.upgradeWeapon(USER_ID);

        assertThat(deletedWeapons).containsExactly(original);
    }

    @Test
    void 強化第二次變成加2() {
        player.addMoney(500);
        storeService.upgradeWeapon(USER_ID);
        storeService.upgradeWeapon(USER_ID);

        assertThat(player.getWeapon().getName()).isEqualTo("初階獵刀(+2)");
        assertThat(player.getWeapon().getAttackBonus()).isEqualTo(35);
    }
}

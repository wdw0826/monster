package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.PotionType;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Weapon;
import com.example.monsterhunter.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreServiceTest {

    private static final Long USER_ID = 7L;

    private StoreService storeService;
    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(USER_ID, "測試獵人");            // 金錢 500
        player.equipWeapon(new Weapon("初階獵刀", 5));
        PlayerRepository playerRepository = Fakes.of(PlayerRepository.class, "findByUserId",
                args -> USER_ID.equals(args[0]) ? Optional.of(player) : Optional.empty());
        storeService = new StoreService(playerRepository);
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
    void 強化第二次變成加2() {
        player.addMoney(500);
        storeService.upgradeWeapon(USER_ID);
        storeService.upgradeWeapon(USER_ID);

        assertThat(player.getWeapon().getName()).isEqualTo("初階獵刀(+2)");
        assertThat(player.getWeapon().getAttackBonus()).isEqualTo(35);
    }
}

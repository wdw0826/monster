package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.BattleActionRequest;
import com.example.monsterhunter.dto.BattleActionType;
import com.example.monsterhunter.dto.BattleResultResponse;
import com.example.monsterhunter.entity.Monster;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.Weapon;
import com.example.monsterhunter.entity.enums.QuestRank;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.repository.PlayerRepository;
import com.example.monsterhunter.repository.QuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BattleService 的單元測試：不需要資料庫、Redis，Repository 用 Fakes（JDK Proxy）假造。
 * 在 IntelliJ 裡點 class 名稱旁的綠色箭頭就能跑。
 */
class BattleServiceTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long QUEST_ID = 10L;

    private BattleService battleService;

    private Player player;
    private Quest quest;

    @BeforeEach
    void setUp() {
        player = new Player(100L, "測試獵人");           // 攻擊 10、HP 100、大藥水 3
        player.equipWeapon(new Weapon("初階獵刀", 5));     // 總攻擊 15

        PlayerRepository playerRepository = Fakes.of(PlayerRepository.class, "findById",
                args -> PLAYER_ID.equals(args[0]) ? Optional.of(player) : Optional.empty());
        QuestRepository questRepository = Fakes.of(QuestRepository.class, "findById",
                args -> QUEST_ID.equals(args[0]) ? Optional.ofNullable(quest) : Optional.empty());
        // evictQuestBoardCache() 本身是空方法（只靠 @CacheEvict），單元測試裡直接用真的 QuestService 就好
        QuestService questService = new QuestService(questRepository);

        battleService = new BattleService(playerRepository, questRepository, questService);
    }

    private Quest questWith(Monster monster, Long activePlayerId) {
        quest = new Quest("測試任務", QuestRank.ONE_STAR, monster);
        if (activePlayerId != null) {
            quest.start(activePlayerId);
        }
        return quest;
    }

    private BattleResultResponse act(BattleActionType action) {
        BattleActionRequest request = new BattleActionRequest();
        request.setQuestId(QUEST_ID);
        request.setAction(action);
        return battleService.performAction(PLAYER_ID, request);
    }

    @Test
    void 攻擊造成基礎攻擊加武器的傷害_魔物反擊() {
        Monster monster = new Monster("青熊獸", 60, 5, 30);
        questWith(monster, PLAYER_ID);

        BattleResultResponse result = act(BattleActionType.ATTACK);

        assertThat(monster.getHp()).isEqualTo(45);          // 60 - 15
        assertThat(player.getHp()).isEqualTo(95);           // 100 - 5
        assertThat(result.isBattleOver()).isFalse();
    }

    @Test
    void 喝藥水那回合魔物傷害減半() {
        Monster monster = new Monster("雌火龍", 150, 18, 120);
        questWith(monster, PLAYER_ID);
        player.setHp(40);

        act(BattleActionType.BIG_POTION);

        assertThat(player.getHp()).isEqualTo(40 + 50 - 9);  // 回 50，受 18/2
        assertThat(player.getBigPotions()).isEqualTo(2);
    }

    @Test
    void 擊敗魔物_任務釋放_拿到經驗金錢_魔物補滿血() {
        Monster monster = new Monster("草食龍", 10, 2, 15);
        Quest quest = questWith(monster, PLAYER_ID);

        BattleResultResponse result = act(BattleActionType.ATTACK);

        assertThat(result.isVictory()).isTrue();
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.AVAILABLE);
        assertThat(quest.getActivePlayerId()).isNull();
        assertThat(monster.getHp()).isEqualTo(monster.getMaxHp());
        assertThat(player.getExp()).isEqualTo(50);
        assertThat(player.getMoney()).isEqualTo(500 + 150);
    }

    @Test
    void 被打倒_任務釋放_HP回到30() {
        Monster monster = new Monster("轟龍", 400, 50, 350);
        Quest quest = questWith(monster, PLAYER_ID);
        player.setHp(20);

        BattleResultResponse result = act(BattleActionType.ATTACK);

        assertThat(result.isDefeated()).isTrue();
        assertThat(player.getHp()).isEqualTo(30);
        assertThat(quest.getActivePlayerId()).isNull();
    }

    @Test
    void 不是自己接的任務不能打() {
        questWith(new Monster("青熊獸", 60, 5, 30), 999L);

        assertThatThrownBy(() -> act(BattleActionType.ATTACK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("其他獵人");
    }

    @Test
    void 還沒接的任務不能打() {
        questWith(new Monster("青熊獸", 60, 5, 30), null);

        assertThatThrownBy(() -> act(BattleActionType.ATTACK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("請先接下此任務");
    }
}

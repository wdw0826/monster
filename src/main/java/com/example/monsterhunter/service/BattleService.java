package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.BattleActionRequest;
import com.example.monsterhunter.dto.BattleActionType;
import com.example.monsterhunter.dto.BattleResultResponse;
import com.example.monsterhunter.dto.MonsterResponse;
import com.example.monsterhunter.dto.PlayerResponse;
import com.example.monsterhunter.entity.Monster;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.exception.ResourceNotFoundException;
import com.example.monsterhunter.repository.PlayerRepository;
import com.example.monsterhunter.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 回合制戰鬥的核心邏輯，是整個遊戲最複雜的一個 Service。一次 performAction() 呼叫
 * 等於玩家出一次招（攻擊/喝藥水/離開），Controller 重複呼叫直到回傳的 battleOver=true。
 * playerId 是 Controller 從登入的 JWT 帶進來的，不是外部直接傳入，所以這裡不用再驗證
 * 「這真的是你本人嗎」，但仍要驗證這個玩家是不是任務目前的 activePlayerId，
 * 避免其他人趁任務還在進行時插手同一場戰鬥、干擾魔物血量。
 */
@Service
public class BattleService {

    private static final int SMALL_POTION_HEAL = 20;
    private static final int BIG_POTION_HEAL = 50;
    private static final int VICTORY_EXP = 50;
    private static final int VICTORY_MONEY = 150;
    private static final int DEFEAT_RECOVER_HP = 30;

    private final PlayerRepository playerRepository;
    private final QuestRepository questRepository;
    private final QuestService questService;

    public BattleService(PlayerRepository playerRepository, QuestRepository questRepository, QuestService questService) {
        this.playerRepository = playerRepository;
        this.questRepository = questRepository;
        this.questService = questService;
    }

    @Transactional
    public BattleResultResponse performAction(Long playerId, BattleActionRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id=" + playerId + " 的玩家"));
        Quest quest = questRepository.findById(request.getQuestId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id=" + request.getQuestId() + " 的任務"));

        if (quest.getStatus() != QuestStatus.IN_PROGRESS) {
            throw new IllegalStateException("請先接下此任務 (POST /api/quests/{id}/accept) 才能開始戰鬥");
        }
        if (!playerId.equals(quest.getActivePlayerId())) {
            throw new IllegalStateException("這個任務目前是其他獵人在進行中，無法加入戰鬥");
        }

        Monster monster = quest.getMonster();
        List<String> messages = new ArrayList<>();

        if (player.getHp() <= 0 || monster.getHp() <= 0) {
            throw new IllegalStateException("這場戰鬥已經結束，請重新接任務");
        }

        BattleActionType action = request.getAction();

        // --- 離開戰鬥 ---
        if (action == BattleActionType.LEAVE) {
            messages.add("離開戰鬥，返回主畫面");
            releaseQuest(quest, monster);
            questRepository.save(quest);
            return buildResult(messages, player, monster, true, false, false, true);
        }

        boolean isDefending = false;

        // --- 玩家回合 ---
        if (action == BattleActionType.ATTACK) {
            int dmg = player.getTotalAttack();
            messages.add("玩家攻擊造成 " + dmg + " 點傷害");
            monster.takeDamage(dmg);
        } else if (action == BattleActionType.SMALL_POTION || action == BattleActionType.BIG_POTION) {
            boolean isSmall = action == BattleActionType.SMALL_POTION;
            boolean hasPotion = isSmall ? player.getSmallPotions() > 0 : player.getBigPotions() > 0;

            if (hasPotion) {
                int healAmount = isSmall ? SMALL_POTION_HEAL : BIG_POTION_HEAL;
                player.heal(healAmount);
                if (isSmall) {
                    player.subSmallPotions(1);
                } else {
                    player.subBigPotions(1);
                }
                isDefending = true;
                messages.add(">> 喝水中！本回合無法攻擊，但獲得 50% 減傷效果！(回復 " + healAmount + " 點 HP)");
            } else {
                messages.add(">> 沒藥水了！強制改為攻擊！");
                int dmg = player.getTotalAttack();
                monster.takeDamage(dmg);
                messages.add("玩家攻擊造成 " + dmg + " 點傷害");
            }
        }

        // --- 魔物回合 ---
        if (monster.getHp() > 0) {
            int originalDmg = monster.getAttack();
            int finalDmg = originalDmg;
            if (isDefending) {
                finalDmg = originalDmg / 2;
                messages.add(">> [防禦生效] 玩家採取喝水姿勢，抵擋了 50% 的傷害！");
            }
            player.takeDamage(finalDmg);
            messages.add(monster.getName() + " 發動攻擊！玩家受到 " + finalDmg + " 點傷害，剩餘 HP: " + player.getHp());
        }

        boolean victory = player.getHp() > 0 && monster.getHp() <= 0;
        boolean defeated = player.getHp() <= 0;
        boolean battleOver = victory || defeated;

        if (victory) {
            messages.add("★ 狩獵成功！ ★");
            releaseQuest(quest, monster);

            player.gainExp(VICTORY_EXP);
            player.addMoney(VICTORY_MONEY);

            int lostHp = player.getMaxHp() - player.getHp();
            int recoverAmount = lostHp / 2;
            player.heal(recoverAmount);
            messages.add("戰後休整：回復了剩餘血量的 50% (" + recoverAmount + " 點)。當前 HP: " + player.getHp());
        } else if (defeated) {
            messages.add("狩獵失敗！即將退回主畫面，並將血量回復至 " + DEFEAT_RECOVER_HP);
            releaseQuest(quest, monster);
            player.setHp(DEFEAT_RECOVER_HP);
        }

        playerRepository.save(player);
        questRepository.save(quest);

        return buildResult(messages, player, monster, battleOver, victory, defeated, false);
    }

    /**
     * 戰鬥結束（離開/勝利/落敗）時共同要做的事：任務放回可接狀態、魔物補血滿，讓下一個人可以再接。
     * 這裡會讓任務板的內容跟著變，記得清掉 QuestService 那邊的 Redis 快取，
     * 不然玩家會在任務板上一直看到這隻明明已經打完的任務還是「有人在打」。
     */
    private void releaseQuest(Quest quest, Monster monster) {
        quest.resetStatus();
        monster.respawn();
        questService.evictQuestBoardCache();
    }

    private BattleResultResponse buildResult(List<String> messages, Player player, Monster monster,
                                              boolean battleOver, boolean victory, boolean defeated, boolean left) {
        return new BattleResultResponse(
                messages,
                new PlayerResponse(player),
                new MonsterResponse(monster),
                battleOver,
                victory,
                defeated,
                left
        );
    }
}

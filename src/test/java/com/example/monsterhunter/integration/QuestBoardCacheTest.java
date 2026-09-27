package com.example.monsterhunter.integration;

import com.example.monsterhunter.dto.BattleActionRequest;
import com.example.monsterhunter.dto.BattleActionType;
import com.example.monsterhunter.dto.QuestResponse;
import com.example.monsterhunter.entity.User;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.repository.UserRepository;
import com.example.monsterhunter.service.BattleService;
import com.example.monsterhunter.service.PlayerService;
import com.example.monsterhunter.service.QuestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 重現並防止「任務板快取過時」：
 *
 * 戰鬥結束時任務會被放回 AVAILABLE，同時要清掉任務板快取。如果「清快取」發生在交易 commit 之前，
 * 中間這段空檔有人查任務板，會從資料庫讀到還沒 commit 的舊狀態（IN_PROGRESS）並重新寫回快取；
 * commit 之後快取裡就一直是舊的，任務明明空出來了，任務板卻顯示有人在打（直到快取 TTL 到期）。
 *
 * 這個測試用一個外層交易把戰鬥動作包起來，在 commit 之前從另一個執行緒查任務板，
 * 模擬「剛好在空檔進來的請求」，再檢查 commit 之後任務板是不是正確的。
 */
@SpringBootTest
@ActiveProfiles("test")
class QuestBoardCacheTest extends TestDatabaseSupport {

    private static final long QUEST_ID = 1L;

    @Autowired
    private QuestService questService;
    @Autowired
    private BattleService battleService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 戰鬥結束後任務板不會卡在過時的進行中狀態() {
        Long playerId = createPlayer();

        questService.acceptQuest(QUEST_ID, playerId);
        assertThat(statusOnBoard(questService.getAvailableQuests())).isEqualTo(QuestStatus.IN_PROGRESS);

        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            BattleActionRequest leave = new BattleActionRequest();
            leave.setQuestId(QUEST_ID);
            leave.setAction(BattleActionType.LEAVE);
            battleService.performAction(playerId, leave);

            // 交易還沒 commit：另一個請求（另一條連線）這時候來查任務板
            CompletableFuture.supplyAsync(questService::getAvailableQuests).join();
        });

        // commit 之後，任務板必須反映真正的狀態
        assertThat(statusOnBoard(questService.getAvailableQuests()))
                .as("任務已經釋放，任務板卻還是顯示進行中（快取在 commit 前就被清掉、又被寫回舊資料）")
                .isEqualTo(QuestStatus.AVAILABLE);
    }

    @Test
    void 接任務後任務板立刻看得到進行中() {
        Long playerId = createPlayer();
        assertThat(statusOnBoard(questService.getAvailableQuests())).isEqualTo(QuestStatus.AVAILABLE);

        questService.acceptQuest(QUEST_ID, playerId);

        assertThat(statusOnBoard(questService.getAvailableQuests())).isEqualTo(QuestStatus.IN_PROGRESS);
    }

    private QuestStatus statusOnBoard(List<QuestResponse> board) {
        return board.stream()
                .filter(q -> q.getId() == QUEST_ID)
                .findFirst()
                .orElseThrow()
                .getStatus();
    }

    private Long createPlayer() {
        String name = "cache" + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.test")
                .password("not-used-in-this-test")
                .enabled(true)
                .build());
        return playerService.createPlayer(user.getId(), name).getId();
    }
}

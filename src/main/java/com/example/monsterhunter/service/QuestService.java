package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.QuestResponse;
import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.exception.ResourceNotFoundException;
import com.example.monsterhunter.repository.QuestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 任務板查詢與接任務邏輯。一個任務同時間只能被一個玩家進行——接任務時會檢查
 * 狀態是不是 AVAILABLE，不是的話代表已經有人在打，直接丟例外擋下來（見 Quest.activePlayerId）。
 *
 * 任務板（getAvailableQuests）查詢頻繁但變動不算頻繁，快取在 Redis 裡的 "questBoard"；
 * 快取的是 QuestResponse（DTO），不是 Quest entity——entity 帶 Hibernate 的 lazy proxy，
 * 直接丟進 Redis 序列化容易出問題，也不該讓持久層物件外洩到快取層。
 * 任何會改到任務狀態的地方（接任務、戰鬥結束）都要記得清快取，不然玩家會看到過期的任務板。
 *
 * 清快取一律等交易 commit 之後才做（見 evictQuestBoardCache），不用 @CacheEvict：
 * @CacheEvict 會在方法一結束、交易還沒 commit 時就清掉快取，這段空檔如果有人查任務板，
 * 會從資料庫讀到還沒 commit 的舊狀態、又寫回快取，任務明明空出來了任務板卻一直顯示「有人在打」
 * （QuestBoardCacheTest 重現過）。getAvailableQuests() 沒有參數，快取裡只有一筆，所以直接整個 clear()。
 */
@Slf4j
@Service
public class QuestService {

    private static final String QUEST_BOARD_CACHE = "questBoard";

    private final QuestRepository questRepository;
    private final CacheManager cacheManager;

    public QuestService(QuestRepository questRepository, CacheManager cacheManager) {
        this.questRepository = questRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(QUEST_BOARD_CACHE)
    public List<QuestResponse> getAvailableQuests() {
        return questRepository.findByUnlockedTrue().stream()
                .map(QuestResponse::new)
                .toList();
    }

    public Quest getQuestOrThrow(Long id) {
        return questRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id=" + id + " 的任務"));
    }

    @Transactional
    public Quest acceptQuest(Long id, Long playerId) {
        Quest quest = getQuestOrThrow(id);
        if (!quest.isUnlocked()) {
            throw new IllegalStateException("此任務尚未解鎖");
        }
        if (quest.getStatus() != QuestStatus.AVAILABLE) {
            throw new IllegalStateException("此任務目前已經有其他獵人在進行中，請稍後再試");
        }
        if (questRepository.existsByActivePlayerId(playerId)) {
            throw new IllegalStateException("你已經有一個進行中的任務，請先完成或離開該任務再接新的");
        }
        quest.start(playerId);
        evictQuestBoardCache();
        return questRepository.save(quest);
    }

    /**
     * 管理端用：強制把卡住的任務放回可接狀態（魔物補滿血）。
     * 玩家接了任務後不再呼叫戰鬥 API（關掉視窗、token 過期、忘記密碼…），任務就會一直是 IN_PROGRESS，
     * 其他人永遠接不到——實際測試時就遇過。這支給管理員手動解開，不用進資料庫改。
     */
    @Transactional
    public Quest forceRelease(Long id) {
        Quest quest = getQuestOrThrow(id);
        quest.resetStatus();
        quest.getMonster().respawn();
        evictQuestBoardCache();
        return quest;
    }

    /**
     * 清掉任務板快取。在交易裡呼叫時，會等到交易 commit 成功之後才真的清
     * （交易 rollback 的話資料沒變，也就不用清）；不在交易裡就立刻清。
     * BattleService 在戰鬥結束（離開/勝利/落敗）時也會呼叫這支。
     *
     * 清快取失敗（例如 Redis 連不上）只記 warn：快取有 5 分鐘 TTL 會自己過期，
     * 而且這時候資料已經 commit 了，不能因為快取出錯就讓整個請求回 500。
     */
    public void evictQuestBoardCache() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clearQuestBoardCache();
                }
            });
        } else {
            clearQuestBoardCache();
        }
    }

    private void clearQuestBoardCache() {
        try {
            Cache cache = cacheManager.getCache(QUEST_BOARD_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } catch (RuntimeException e) {
            log.warn("清除任務板快取失敗，等 TTL 到期自動更新：{}", e.getMessage());
        }
    }
}

package com.example.monsterhunter.service;

import com.example.monsterhunter.dto.QuestResponse;
import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.exception.ResourceNotFoundException;
import com.example.monsterhunter.repository.QuestRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * @CacheEvict 這裡都要記得加 allEntries = true：getAvailableQuests() 沒有參數，key 是固定的
 * 空 key，如果 evict 那邊沒加 allEntries，預設會拿「被標記方法自己的參數」去算 key
 * （例如 acceptQuest 的 id、playerId），跟空 key 對不上，等於什麼都沒清到——
 * 實測時就是這樣：接了任務，畫面卻還看到接之前的舊任務板，查了才發現是這個。
 */
@Service
public class QuestService {

    private static final String QUEST_BOARD_CACHE = "questBoard";

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
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
    @CacheEvict(value = QUEST_BOARD_CACHE, allEntries = true)
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
        return questRepository.save(quest);
    }

    /** 給 BattleService 用：戰鬥結束（離開/勝利/落敗）會讓任務重新變成 AVAILABLE，任務板快取要一起清掉。 */
    @CacheEvict(value = QUEST_BOARD_CACHE, allEntries = true)
    public void evictQuestBoardCache() {
    }
}

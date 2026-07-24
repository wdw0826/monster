package com.example.monsterhunter.service;

import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.enums.QuestStatus;
import com.example.monsterhunter.exception.ResourceNotFoundException;
import com.example.monsterhunter.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任務板查詢與接任務邏輯。一個任務同時間只能被一個玩家進行——接任務時會檢查
 * 狀態是不是 AVAILABLE，不是的話代表已經有人在打，直接丟例外擋下來（見 Quest.activePlayerId）。
 */
@Service
public class QuestService {

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    public List<Quest> getAvailableQuests() {
        return questRepository.findByUnlockedTrue();
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
        quest.start(playerId);
        return questRepository.save(quest);
    }
}

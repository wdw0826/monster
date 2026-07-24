package com.example.monsterhunter.dto;

import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.entity.enums.QuestRank;
import com.example.monsterhunter.entity.enums.QuestStatus;

/**
 * 回傳給前端的任務資料格式，把 Quest entity（含掛的魔物）整理成 API 回應用的欄位。
 * activePlayerId 是 null 代表沒人在打、可以接；不是 null 代表已經有玩家在進行中。
 */
public class QuestResponse {
    private Long id;
    private String name;
    private QuestRank rank;
    private QuestStatus status;
    private boolean unlocked;
    private MonsterResponse monster;
    private Long activePlayerId;

    public QuestResponse() {
    }

    public QuestResponse(Quest quest) {
        this.id = quest.getId();
        this.name = quest.getName();
        this.rank = quest.getRank();
        this.status = quest.getStatus();
        this.unlocked = quest.isUnlocked();
        this.monster = new MonsterResponse(quest.getMonster());
        this.activePlayerId = quest.getActivePlayerId();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public QuestRank getRank() {
        return rank;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public MonsterResponse getMonster() {
        return monster;
    }

    public Long getActivePlayerId() {
        return activePlayerId;
    }
}

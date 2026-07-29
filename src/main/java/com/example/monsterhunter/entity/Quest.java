package com.example.monsterhunter.entity;

import com.example.monsterhunter.entity.enums.QuestRank;
import com.example.monsterhunter.entity.enums.QuestStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 任務，掛一隻魔物（見 Monster）。任務板一開始就種好 9 個（1★/2★/3★ 各 3 個），
 * 全部預設解鎖，玩家可以直接接。同時間只能有一個玩家在進行同一個任務——
 * activePlayerId 記錄「現在是誰在打」，接任務時寫入、戰鬥結束（贏/輸/離開）時清空，
 * 避免兩個帳號同時打同一隻魔物、血量互相干擾。
 */
@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private QuestRank rank;

    @Enumerated(EnumType.STRING)
    private QuestStatus status;

    // fetch 改 LAZY：預設的 EAGER 會讓 GET /api/quests 這種列表 API 產生 N+1
    // （查完任務列表後，每一筆再各自補一條查 monster 的 SQL），改用 Repository 端的
    // JOIN FETCH 來一次撈好，見 QuestRepository.findByUnlockedTrue()。
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id")
    private Monster monster;

    private boolean unlocked;

    @Column(name = "active_player_id")
    private Long activePlayerId;

    protected Quest() {
    }

    public Quest(String name, QuestRank rank, Monster monster) {
        this.name = name;
        this.rank = rank;
        this.monster = monster;
        this.status = QuestStatus.AVAILABLE;
        this.unlocked = false;
    }

    public void start(Long playerId) {
        status = QuestStatus.IN_PROGRESS;
        activePlayerId = playerId;
    }

    public void resetStatus() {
        this.status = QuestStatus.AVAILABLE;
        this.activePlayerId = null;
    }

    public Long getId() {
        return id;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public QuestRank getRank() {
        return rank;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public Monster getMonster() {
        return monster;
    }

    public String getName() {
        return name;
    }

    public Long getActivePlayerId() {
        return activePlayerId;
    }
}

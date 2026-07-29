package com.example.monsterhunter.repository;

import com.example.monsterhunter.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Quest 的資料庫存取介面。多加了「只找出已解鎖的任務」（findByUnlockedTrue），
 * 對應任務板 API 只顯示玩家目前能接的任務。
 * monster 欄位改成 LAZY 之後，這裡改用 JOIN FETCH 明確一次撈好，
 * 避免列表 API 每筆任務各補一條查 monster 的 SQL（N+1）。
 */
public interface QuestRepository extends JpaRepository<Quest, Long> {
    @Query("SELECT q FROM Quest q JOIN FETCH q.monster WHERE q.unlocked = true")
    List<Quest> findByUnlockedTrue();

    boolean existsByActivePlayerId(Long activePlayerId);
}

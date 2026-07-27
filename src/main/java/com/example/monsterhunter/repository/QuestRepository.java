package com.example.monsterhunter.repository;

import com.example.monsterhunter.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Quest 的資料庫存取介面。多加了「只找出已解鎖的任務」（findByUnlockedTrue），
 * 對應任務板 API 只顯示玩家目前能接的任務，方法名稱本身就是查詢條件，
 * Spring Data JPA 會自動幫忙產生對應的 SQL。
 */
public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findByUnlockedTrue();

    boolean existsByActivePlayerId(Long activePlayerId);
}

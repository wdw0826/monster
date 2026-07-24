package com.example.monsterhunter.repository;

import com.example.monsterhunter.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Player 的資料庫存取介面，繼承 JpaRepository 就自動有 save/findById/delete 等基本方法，
 * 不用自己寫 SQL。多加了「用 userId 找到這個帳號的獵人」，因為 API 都是靠登入者的 userId
 * 找角色，而不是直接拿 playerId。
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}

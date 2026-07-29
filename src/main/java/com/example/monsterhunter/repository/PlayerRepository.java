package com.example.monsterhunter.repository;

import com.example.monsterhunter.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Player 的資料庫存取介面，繼承 JpaRepository 就自動有 save/findById/delete 等基本方法，
 * 不用自己寫 SQL。多加了「用 userId 找到這個帳號的獵人」，因為 API 都是靠登入者的 userId
 * 找角色，而不是直接拿 playerId。
 * weapon 欄位改成 LAZY 之後，管理端列出所有玩家（AdminController）改用
 * findAllWithWeapon() 的 JOIN FETCH 一次撈好，避免每個玩家各補一條查 weapon 的 SQL（N+1）。
 * LEFT JOIN 是因為 weapon_id 允許 null（雖然目前建立獵人時一定會配一把武器）。
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.weapon")
    List<Player> findAllWithWeapon();
}

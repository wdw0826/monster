package com.example.monsterhunter.service;

import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Weapon;
import com.example.monsterhunter.exception.ResourceNotFoundException;
import com.example.monsterhunter.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 獵人角色的建立與查詢。一律以「目前登入帳號的 userId」為準，不接受呼叫端指定別人的角色 id。
 * 新建立的獵人會自動配一把「初階獵刀」，一個帳號只能建立一次（見 createPlayer 的重複檢查）。
 */
@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player createPlayer(Long userId, String name) {
        if (playerRepository.existsByUserId(userId)) {
            throw new IllegalStateException("這個帳號已經有獵人角色了");
        }
        Player player = new Player(userId, name);
        player.equipWeapon(new Weapon("初階獵刀", 5));
        return playerRepository.save(player);
    }

    public Player getMyPlayerOrThrow(Long userId) {
        return playerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("這個帳號還沒有獵人角色，請先 POST /api/players/me 建立"));
    }

    /** 管理端用：列出所有玩家的獵人角色，一般使用者拿不到這個方法，只有 AdminController 會呼叫。 */
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
}

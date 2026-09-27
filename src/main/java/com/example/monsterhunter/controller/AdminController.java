package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.PlayerResponse;
import com.example.monsterhunter.dto.QuestResponse;
import com.example.monsterhunter.service.PlayerService;
import com.example.monsterhunter.service.QuestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端點，只有 ROLE_ADMIN 的帳號能呼叫（規則寫在 SecurityConfig 的 /api/admin/** 那條）。
 * 一般使用者（ROLE_USER）打這裡會被 Spring Security 擋掉，直接回 403，連進到這支方法都不會。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PlayerService playerService;
    private final QuestService questService;

    public AdminController(PlayerService playerService, QuestService questService) {
        this.playerService = playerService;
        this.questService = questService;
    }

    /** 列出所有玩家的獵人角色，一般使用者的 GET /api/players/me 只看得到自己的。 */
    @GetMapping("/players")
    public List<PlayerResponse> listAllPlayers() {
        return playerService.getAllPlayers().stream()
                .map(PlayerResponse::new)
                .toList();
    }

    /** 砍掉一個玩家的獵人角色（身上裝備的武器由資料庫 trigger 一起刪，見 V4 migration）。手上有進行中任務的話會被擋下來，回 409。帳號本身保留。 */
    @DeleteMapping("/players/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
    }

    /** 強制釋放卡住的任務：狀態改回 AVAILABLE、清掉 activePlayerId、魔物補滿血。 */
    @PostMapping("/quests/{id}/release")
    public QuestResponse releaseQuest(@PathVariable Long id) {
        return new QuestResponse(questService.forceRelease(id));
    }
}

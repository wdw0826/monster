package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.PlayerResponse;
import com.example.monsterhunter.service.PlayerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public AdminController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /** 列出所有玩家的獵人角色，一般使用者的 GET /api/players/me 只看得到自己的。 */
    @GetMapping("/players")
    public List<PlayerResponse> listAllPlayers() {
        return playerService.getAllPlayers().stream()
                .map(PlayerResponse::new)
                .toList();
    }
}

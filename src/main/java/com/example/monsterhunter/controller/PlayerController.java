package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.CreatePlayerRequest;
import com.example.monsterhunter.dto.PlayerResponse;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.security.UserPrincipal;
import com.example.monsterhunter.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 獵人角色管理的 API 入口：建立自己的獵人、查自己的角色狀態。
 * 全部端點都需要先登入拿到 JWT，操作對象永遠是「目前登入帳號自己的獵人」——
 * 玩家身分是從 @AuthenticationPrincipal 解出來的，request 裡不會、也不能指定要操作誰。
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createMyPlayer(@AuthenticationPrincipal UserPrincipal user,
                                          @Valid @RequestBody CreatePlayerRequest request) {
        Player player = playerService.createPlayer(user.getId(), request.getName());
        return new PlayerResponse(player);
    }

    @GetMapping("/me")
    public PlayerResponse getMyPlayer(@AuthenticationPrincipal UserPrincipal user) {
        return new PlayerResponse(playerService.getMyPlayerOrThrow(user.getId()));
    }
}

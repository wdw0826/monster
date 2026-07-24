package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.BattleActionRequest;
import com.example.monsterhunter.dto.BattleResultResponse;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.security.UserPrincipal;
import com.example.monsterhunter.service.BattleService;
import com.example.monsterhunter.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回合制戰鬥的 API 入口。一次 POST /api/battles/action 等於玩家出一次招（攻擊/喝藥水/離開），
 * 重複呼叫直到回應裡的 battleOver=true 為止。這裡只負責「從 JWT 拿到目前登入者、
 * 換成他自己的獵人 id」，實際的傷害計算、勝敗判定都交給 BattleService 處理。
 */
@RestController
@RequestMapping("/api/battles")
public class BattleController {

    private final BattleService battleService;
    private final PlayerService playerService;

    public BattleController(BattleService battleService, PlayerService playerService) {
        this.battleService = battleService;
        this.playerService = playerService;
    }

    @PostMapping("/action")
    public BattleResultResponse performAction(@AuthenticationPrincipal UserPrincipal user,
                                               @Valid @RequestBody BattleActionRequest request) {
        Player player = playerService.getMyPlayerOrThrow(user.getId());
        return battleService.performAction(player.getId(), request);
    }
}

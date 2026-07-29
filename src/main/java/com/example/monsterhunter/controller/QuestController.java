package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.QuestResponse;
import com.example.monsterhunter.entity.Player;
import com.example.monsterhunter.entity.Quest;
import com.example.monsterhunter.security.UserPrincipal;
import com.example.monsterhunter.service.PlayerService;
import com.example.monsterhunter.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任務板的 API 入口。GET 系列（看任務列表、看單一任務詳情）是公開的，
 * 不用登入也能瀏覽；接任務（POST .../accept）要登入，會用目前登入者自己的獵人去接，
 * 並把這個玩家記在任務上（Quest.activePlayerId），擋掉別人在戰鬥還沒結束前搶同一個任務。
 */
@Tag(name = "Quest", description = "任務板：查詢任務、接任務")
@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;
    private final PlayerService playerService;

    public QuestController(QuestService questService, PlayerService playerService) {
        this.questService = questService;
        this.playerService = playerService;
    }

    @GetMapping
    public List<QuestResponse> listQuests() {
        return questService.getAvailableQuests();
    }

    @GetMapping("/{id}")
    public QuestResponse getQuest(@PathVariable Long id) {
        return new QuestResponse(questService.getQuestOrThrow(id));
    }

    @Operation(
            summary = "接任務",
            description = "用目前登入者自己的獵人接下這個任務，接成功後任務狀態變 IN_PROGRESS。"
                    + "任務已經有人在打、還沒解鎖、或這個玩家自己已經有進行中的任務，都會回 409。"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/accept")
    public QuestResponse acceptQuest(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        Player player = playerService.getMyPlayerOrThrow(user.getId());
        Quest quest = questService.acceptQuest(id, player.getId());
        return new QuestResponse(quest);
    }
}

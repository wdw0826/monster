package com.example.monsterhunter.dto;

import jakarta.validation.constraints.NotNull;

/**
 * POST /api/battles/action 的請求格式：要打哪個任務（questId）、這回合下什麼指令（action）。
 * 玩家身分不是這個物件的一部分——是從登入的 JWT 拿的（Controller 用 @AuthenticationPrincipal），
 * 避免呼叫端自己填一個 playerId 冒充別人。
 */
public class BattleActionRequest {

    @NotNull(message = "questId 不可為空")
    private Long questId;

    @NotNull(message = "action 不可為空")
    private BattleActionType action;

    public BattleActionRequest() {
    }

    public Long getQuestId() {
        return questId;
    }

    public void setQuestId(Long questId) {
        this.questId = questId;
    }

    public BattleActionType getAction() {
        return action;
    }

    public void setAction(BattleActionType action) {
        this.action = action;
    }
}

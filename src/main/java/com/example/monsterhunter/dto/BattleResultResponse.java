package com.example.monsterhunter.dto;

import java.util.List;

/**
 * 一次戰鬥回合結束後回傳給前端的結果：這回合發生了什麼事（messages，例如「造成 15 點傷害」），
 * 玩家與魔物的最新狀態，以及這場戰鬥有沒有結束（battleOver）——
 * 結束的話再看是打贏（victory）、打輸（defeated）、還是自己離開（left）。
 * battleOver 是 false 時代表還沒完，前端要繼續呼叫下一回合。
 */
public class BattleResultResponse {
    private List<String> messages;
    private PlayerResponse player;
    private MonsterResponse monster;
    private boolean battleOver;
    private boolean victory;
    private boolean defeated;
    private boolean left;

    public BattleResultResponse() {
    }

    public BattleResultResponse(List<String> messages, PlayerResponse player, MonsterResponse monster,
                                 boolean battleOver, boolean victory, boolean defeated, boolean left) {
        this.messages = messages;
        this.player = player;
        this.monster = monster;
        this.battleOver = battleOver;
        this.victory = victory;
        this.defeated = defeated;
        this.left = left;
    }

    public List<String> getMessages() {
        return messages;
    }

    public PlayerResponse getPlayer() {
        return player;
    }

    public MonsterResponse getMonster() {
        return monster;
    }

    public boolean isBattleOver() {
        return battleOver;
    }

    public boolean isVictory() {
        return victory;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isLeft() {
        return left;
    }
}

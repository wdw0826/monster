package com.example.monsterhunter.entity.enums;

/**
 * 任務目前的狀態：
 *   AVAILABLE   可以被接（沒人在打）
 *   IN_PROGRESS 有玩家正在打（Quest.activePlayerId 會指向那個玩家）
 *   COMPLETED   保留給「任務永久完成、不再重複開放」的情境；目前玩法是不管打贏或打輸，
 *               戰鬥結束後都會重置回 AVAILABLE 讓任務可以重打，所以這個狀態目前還沒被用到。
 */
public enum QuestStatus {
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED
}

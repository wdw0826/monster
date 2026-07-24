package com.example.monsterhunter.entity.enums;

/**
 * 任務難度星級：1★最簡單、3★最強，決定該任務掛的魔物血量/攻擊力高低
 * （實際數值寫在 V2__create_game_schema.sql 的種子資料裡）。
 */
public enum QuestRank {
    ONE_STAR("1★"),
    TWO_STAR("2★"),
    THREE_STAR("3★");

    private final String label;

    QuestRank(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

-- ============================================================
-- V3：併發防護
--
-- 1. quests / players 加上 version 欄位，給 JPA @Version（樂觀鎖）用。
--    兩個請求同時讀到同一筆資料、各自修改後寫回時，後寫的那個會因為 version 對不上而失敗，
--    不會再「兩個人都以為自己接到任務」或「連點買東西，回 200 但沒真的買到」。
--
-- 2. quests.active_player_id 加 partial unique index：
--    「一個獵人同時只能進行一個任務」原本只在 QuestService 用程式碼擋，
--    兩個請求同時進來時兩邊的檢查都會通過。改由資料庫保證，程式碼的檢查只負責給好讀的錯誤訊息。
--    WHERE active_player_id IS NOT NULL：沒人在打的任務（NULL）不受限制，可以有很多筆。
-- ============================================================

ALTER TABLE quests  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_quests_active_player
    ON quests (active_player_id)
    WHERE active_player_id IS NOT NULL;

-- ============================================================
-- V2：魔物獵人遊戲業務資料表 + 任務板種子資料
-- ============================================================

CREATE TABLE weapons (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    attack_bonus  INT NOT NULL
);

CREATE TABLE monsters (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    hp          INT NOT NULL,
    max_hp      INT NOT NULL,
    attack      INT NOT NULL,
    exp_value   INT NOT NULL
);

-- 每個帳號最多一隻獵人：user_id 唯一
CREATE TABLE players (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT NOT NULL UNIQUE,
    name           VARCHAR(50) NOT NULL,
    hp             INT NOT NULL DEFAULT 100,
    max_hp         INT NOT NULL DEFAULT 100,
    attack         INT NOT NULL DEFAULT 10,
    level          INT NOT NULL DEFAULT 1,
    exp            INT NOT NULL DEFAULT 0,
    money          INT NOT NULL DEFAULT 500,
    small_potions  INT NOT NULL DEFAULT 0,
    big_potions    INT NOT NULL DEFAULT 3,
    weapon_id      BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (weapon_id) REFERENCES weapons(id)
);

-- active_player_id：任務目前被哪隻獵人接走了，null 代表沒人在打，
-- 用來擋「兩個玩家同時打同一隻魔物、血量互相干擾」的狀況。
CREATE TABLE quests (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    rank              VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    unlocked          BOOLEAN NOT NULL DEFAULT FALSE,
    monster_id        BIGINT NOT NULL,
    active_player_id  BIGINT,
    FOREIGN KEY (monster_id) REFERENCES monsters(id),
    FOREIGN KEY (active_player_id) REFERENCES players(id)
);

-- 種子資料：9 隻魔物（1★/2★/3★ 各 3 隻），依序對應 id 1~9
INSERT INTO monsters (name, hp, max_hp, attack, exp_value) VALUES
    ('青熊獸', 60, 60, 5, 30),
    ('藍速龍', 40, 40, 8, 25),
    ('草食龍', 30, 30, 2, 15),
    ('雌火龍', 150, 150, 18, 120),
    ('奇猿狐', 120, 120, 22, 110),
    ('岩龍', 200, 200, 12, 130),
    ('轟龍', 400, 400, 50, 350),
    ('雄火龍', 350, 350, 60, 400),
    ('碎龍', 450, 450, 55, 450);

-- 種子資料：9 個任務，全部預設解鎖，monster_id 對應上面依序建立的魔物
INSERT INTO quests (name, rank, status, unlocked, monster_id) VALUES
    ('【1★】青熊獸狩獵', 'ONE_STAR', 'AVAILABLE', TRUE, 1),
    ('【1★】藍速龍狩獵', 'ONE_STAR', 'AVAILABLE', TRUE, 2),
    ('【1★】草食龍狩獵', 'ONE_STAR', 'AVAILABLE', TRUE, 3),
    ('【2★】雌火龍狩獵', 'TWO_STAR', 'AVAILABLE', TRUE, 4),
    ('【2★】奇猿狐狩獵', 'TWO_STAR', 'AVAILABLE', TRUE, 5),
    ('【2★】岩龍狩獵', 'TWO_STAR', 'AVAILABLE', TRUE, 6),
    ('【3★】轟龍狩獵', 'THREE_STAR', 'AVAILABLE', TRUE, 7),
    ('【3★】雄火龍狩獵', 'THREE_STAR', 'AVAILABLE', TRUE, 8),
    ('【3★】碎龍狩獵', 'THREE_STAR', 'AVAILABLE', TRUE, 9);

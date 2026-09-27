-- ============================================================
-- V4：角色被刪掉時，資料庫自己把他的武器一起刪掉
--
-- 外鍵方向是 players.weapon_id → weapons，所以刪 players 時資料庫不會連帶刪 weapons。
-- 以前只有透過 JPA 刪角色（admin API）才會一起刪武器；如果是刪帳號、由資料庫
-- CASCADE 連帶刪掉角色（users → players），武器就會留下來變成沒人用的孤兒——
-- 開發用的資料庫裡實際查到過 14 把。
--
-- 改成由這個 trigger 統一負責「角色刪了 → 武器也刪」，不管角色是從哪條路被刪的都一樣。
-- Java 那邊的 Player.weapon 因此不再設 cascade REMOVE / orphanRemoval：兩邊都刪的話，
-- Hibernate 事後要刪的武器已經被 trigger 刪掉，會以為資料被別人改過而報錯。
-- 強化武器換下來的舊武器則由 StoreService 明確刪除。
-- ============================================================

-- 先清掉之前已經留下來的孤兒武器
DELETE FROM weapons w
WHERE NOT EXISTS (SELECT 1 FROM players p WHERE p.weapon_id = w.id);

CREATE FUNCTION delete_weapon_of_deleted_player() RETURNS trigger AS $$
BEGIN
    IF OLD.weapon_id IS NOT NULL THEN
        DELETE FROM weapons WHERE id = OLD.weapon_id;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_players_delete_weapon
    AFTER DELETE ON players
    FOR EACH ROW
    EXECUTE FUNCTION delete_weapon_of_deleted_player();

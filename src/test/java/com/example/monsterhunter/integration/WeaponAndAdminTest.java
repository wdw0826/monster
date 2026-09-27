package com.example.monsterhunter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 武器的生命週期跟管理端 API。
 *
 * 武器掛在角色身上（players.weapon_id → weapons），角色被刪掉時武器也要一起刪，
 * 不然 weapons 表會累積沒人用的孤兒武器——實際在開發用的資料庫裡查到過 14 把。
 * 不管角色是從哪條路被刪的（admin API、或是直接刪帳號讓資料庫 CASCADE 連帶刪掉角色），
 * 都不能留下孤兒；強化武器換掉的舊武器也一樣。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WeaponAndAdminTest extends TestDatabaseSupport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private record Res(int status, JsonNode json) {
    }

    private record Hunter(long userId, long playerId, long weaponId, String token) {
    }

    // ------------------------------------------------------------------ 孤兒武器

    @Test
    void 直接從資料庫刪帳號_角色跟武器都要一起消失() throws Exception {
        Hunter h = newHunter();

        jdbc.update("DELETE FROM users WHERE id = ?", h.userId);

        assertThat(count("SELECT count(*) FROM players WHERE id = ?", h.playerId)).as("角色被 CASCADE 刪掉").isZero();
        assertThat(count("SELECT count(*) FROM weapons WHERE id = ?", h.weaponId)).as("武器不能留下來變孤兒").isZero();
    }

    @Test
    void 管理員用API刪角色_武器一起刪_帳號保留() throws Exception {
        Hunter h = newHunter();
        String admin = newAdminToken();

        assertThat(send("DELETE", "/api/admin/players/" + h.playerId, null, admin).status).isEqualTo(204);

        assertThat(count("SELECT count(*) FROM players WHERE id = ?", h.playerId)).isZero();
        assertThat(count("SELECT count(*) FROM weapons WHERE id = ?", h.weaponId)).as("武器一起刪").isZero();
        assertThat(count("SELECT count(*) FROM users WHERE id = ?", h.userId)).as("只刪角色，不刪帳號").isOne();
        assertThat(send("GET", "/api/players/me", null, h.token).status).as("帳號還在，可以重新建角").isEqualTo(404);
        assertThat(send("POST", "/api/players/me", Map.of("name", "重新來過"), h.token).status).isEqualTo(201);
    }

    @Test
    void 強化武器後舊武器不會留下() throws Exception {
        Hunter h = newHunter();
        jdbc.update("UPDATE players SET money = 1000 WHERE id = ?", h.playerId);

        Res first = send("POST", "/api/store/upgrade-weapon", null, h.token);
        Res second = send("POST", "/api/store/upgrade-weapon", null, h.token);

        assertThat(first.status).isEqualTo(200);
        assertThat(second.status).isEqualTo(200);
        assertThat(second.json.path("weapon").path("name").asText()).isEqualTo("初階獵刀(+2)");
        assertThat(second.json.path("weapon").path("attackBonus").asInt()).isEqualTo(35);
        assertThat(count("SELECT count(*) FROM weapons WHERE id IN (?, ?)", h.weaponId, first.json.path("weapon").path("id").asLong()))
                .as("被換掉的兩把舊武器都要刪掉").isZero();
        assertThat(orphanWeapons()).isZero();
    }

    @Test
    void 整個流程跑完資料庫裡沒有孤兒武器() throws Exception {
        newHunter();
        Hunter deletedByDb = newHunter();
        jdbc.update("DELETE FROM users WHERE id = ?", deletedByDb.userId);

        assertThat(orphanWeapons()).isZero();
    }

    @Test
    void 兩隻角色不能共用同一把武器() throws Exception {
        Hunter a = newHunter();
        Hunter b = newHunter();
        try {
            // 如果允許共用，刪掉 a 時 trigger 會把武器刪掉，b 就會指向一把不存在的武器
            assertThatThrownBy(() -> jdbc.update("UPDATE players SET weapon_id = ? WHERE id = ?", a.weaponId, b.playerId))
                    .as("資料庫要擋下「兩隻角色指向同一把武器」")
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            // 沒被擋下的話（修正前）把 b 改回原本的武器，不影響後面的測試
            jdbc.update("UPDATE players SET weapon_id = ? WHERE id = ?", b.weaponId, b.playerId);
        }
    }

    // ------------------------------------------------------------------ 管理端 API（需要 ROLE_ADMIN）

    @Test
    void 管理員可以列出所有角色() throws Exception {
        Hunter h = newHunter();
        Res list = send("GET", "/api/admin/players", null, newAdminToken());

        assertThat(list.status).isEqualTo(200);
        assertThat(list.json.findValuesAsText("id")).contains(String.valueOf(h.playerId));
    }

    @Test
    void 管理員可以強制釋放卡住的任務() throws Exception {
        Hunter h = newHunter();
        send("POST", "/api/quests/4/accept", null, h.token);
        send("POST", "/api/battles/action", Map.of("questId", 4, "action", "ATTACK"), h.token);  // 魔物受傷

        Res released = send("POST", "/api/admin/quests/4/release", null, newAdminToken());

        assertThat(released.status).isEqualTo(200);
        assertThat(released.json.path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(released.json.path("activePlayerId").isNull()).isTrue();
        assertThat(released.json.path("monster").path("hp").asInt()).isEqualTo(released.json.path("monster").path("maxHp").asInt());
        assertThat(send("GET", "/api/quests/4", null, null).json.path("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void 手上有進行中任務的角色不能刪() throws Exception {
        Hunter h = newHunter();
        send("POST", "/api/quests/5/accept", null, h.token);

        assertThat(send("DELETE", "/api/admin/players/" + h.playerId, null, newAdminToken()).status).isEqualTo(409);
        assertThat(count("SELECT count(*) FROM weapons WHERE id = ?", h.weaponId)).isOne();
    }

    // ------------------------------------------------------------------ 小工具

    private Hunter newHunter() throws Exception {
        String token = register(false);
        JsonNode player = send("POST", "/api/players/me", Map.of("name", "武器測試"), token).json;
        long playerId = player.path("id").asLong();
        long userId = jdbc.queryForObject("SELECT user_id FROM players WHERE id = ?", Long.class, playerId);
        return new Hunter(userId, playerId, player.path("weapon").path("id").asLong(), token);
    }

    /** 管理員帳號沒辦法透過 API 產生（刻意的），測試裡直接在測試資料庫把 ROLE_ADMIN 綁上去。 */
    private String newAdminToken() throws Exception {
        return register(true);
    }

    private String register(boolean admin) throws Exception {
        String name = "w" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String password = "password-" + UUID.randomUUID();
        send("POST", "/api/auth/register", Map.of("username", name, "email", name + "@example.test", "password", password), null);
        if (admin) {
            jdbc.update("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT u.id, r.id FROM users u, roles r WHERE u.username = ? AND r.name = 'ROLE_ADMIN'
                    """, name);
        }
        return send("POST", "/api/auth/login", Map.of("username", name, "password", password), null)
                .json.path("accessToken").asText();
    }

    private long orphanWeapons() {
        return count("SELECT count(*) FROM weapons w WHERE NOT EXISTS (SELECT 1 FROM players p WHERE p.weapon_id = w.id)");
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private Res send(String method, String path, Object body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json;
        try {
            json = response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
        } catch (Exception notJson) {
            // 例如註冊成功回的是純文字「註冊成功」
            json = JSON.getNodeFactory().textNode(response.body());
        }
        return new Res(response.statusCode(), json);
    }
}

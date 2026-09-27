package com.example.monsterhunter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 整個 API 的端到端測試：真的把 app 起在一個隨機 port 上，用 HTTP 打每一支 API，
 * 資料寫進測試用的 PostgreSQL（monsterhunter_test）。
 *
 * 涵蓋：註冊登入、建角、商店、戰鬥（打贏、升級、離開、落敗）、權限、錯誤輸入、token 換新與登出、
 * 以及兩個併發情境（同時接同一個任務、連點購買）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiFlowTest extends TestDatabaseSupport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private record Res(int status, JsonNode json) {
    }

    private record Account(String username, String password, String accessToken, String refreshToken) {
    }

    // ------------------------------------------------------------------ 帳號

    @Test
    void 註冊登入與輸入驗證() throws Exception {
        String name = uniqueName();
        String password = "password-" + UUID.randomUUID();

        assertThat(post("/api/auth/register", Map.of("username", name, "email", name + "@example.test", "password", password), null).status).isEqualTo(201);
        assertThat(post("/api/auth/register", Map.of("username", name, "email", "other" + name + "@example.test", "password", password), null).status)
                .as("重複帳號").isEqualTo(400);
        assertThat(post("/api/auth/login", Map.of("username", name, "password", "wrong-password"), null).status)
                .as("密碼錯誤").isEqualTo(401);
        assertThat(post("/api/auth/register", Map.of("username", "", "email", "bad", "password", "1"), null).status)
                .as("欄位驗證").isEqualTo(400);

        Res login = post("/api/auth/login", Map.of("username", name, "password", password), null);
        assertThat(login.status).isEqualTo(200);
        assertThat(login.json.path("accessToken").asText()).isNotBlank();
    }

    @Test
    void 名稱或Email超過資料庫長度上限回400而不是409或500() throws Exception {
        // 格式合法（本地部分 ≤ 64、網域每段 ≤ 63），但總長 106 字，超過資料庫上限 100
        String longEmail = "a".repeat(60) + "@" + "b".repeat(40) + ".test";
        assertThat(post("/api/auth/register", Map.of("username", uniqueName(), "email", longEmail, "password", "password123"), null).status)
                .as("Email 太長").isEqualTo(400);

        Account a = newAccount();
        String longName = "獵".repeat(51);                     // 資料庫上限 50
        assertThat(post("/api/players/me", Map.of("name", longName), a.accessToken).status)
                .as("建角名稱太長").isEqualTo(400);

        assertThat(post("/api/players/me", Map.of("name", "正常名稱"), a.accessToken).status).isEqualTo(201);
        assertThat(send("PATCH", "/api/players/me", Map.of("name", longName), a.accessToken).status)
                .as("改名名稱太長").isEqualTo(400);
        assertThat(send("PATCH", "/api/players/me", Map.of("name", "獵".repeat(50)), a.accessToken).status)
                .as("剛好 50 字要能過").isEqualTo(200);
    }

    @Test
    void token換新與登出() throws Exception {
        Account a = newAccount();

        Res refreshed = post("/api/auth/refresh", Map.of("refreshToken", a.refreshToken), null);
        assertThat(refreshed.status).isEqualTo(200);
        assertThat(post("/api/auth/refresh", Map.of("refreshToken", a.refreshToken), null).status)
                .as("舊的 refresh token 要失效").isEqualTo(401);

        String newRefresh = refreshed.json.path("refreshToken").asText();
        assertThat(post("/api/auth/logout", Map.of("refreshToken", newRefresh), null).status).isEqualTo(200);
        assertThat(post("/api/auth/refresh", Map.of("refreshToken", newRefresh), null).status)
                .as("登出後 refresh token 失效").isEqualTo(401);
    }

    // ------------------------------------------------------------------ 角色與商店

    @Test
    void 建角改名與商店() throws Exception {
        Account a = newAccount();
        assertThat(get("/api/players/me", a.accessToken).status).as("還沒建角").isEqualTo(404);

        Res created = post("/api/players/me", Map.of("name", "測試獵人"), a.accessToken);
        assertThat(created.status).isEqualTo(201);
        assertThat(created.json.path("money").asInt()).isEqualTo(500);
        assertThat(created.json.path("hp").asInt()).isEqualTo(100);
        assertThat(created.json.path("totalAttack").asInt()).isEqualTo(15);
        assertThat(post("/api/players/me", Map.of("name", "第二隻"), a.accessToken).status).as("重複建角").isEqualTo(409);

        Res renamed = send("PATCH", "/api/players/me", Map.of("name", "改名獵人"), a.accessToken);
        assertThat(renamed.json.path("name").asText()).isEqualTo("改名獵人");

        Res small = post("/api/store/potion?type=SMALL", null, a.accessToken);
        assertThat(small.json.path("money").asInt()).isEqualTo(400);
        assertThat(small.json.path("smallPotions").asInt()).isEqualTo(1);
        Res big = post("/api/store/potion?type=BIG", null, a.accessToken);
        assertThat(big.json.path("money").asInt()).isEqualTo(200);
        assertThat(big.json.path("bigPotions").asInt()).isEqualTo(4);
        assertThat(post("/api/store/upgrade-weapon", null, a.accessToken).status).as("錢不夠強化").isEqualTo(409);
    }

    // ------------------------------------------------------------------ 戰鬥

    @Test
    void 打贏兩場升級後強化武器() throws Exception {
        Account a = newAccountWithPlayer();

        assertThat(battle(a, 2, "ATTACK").status).as("沒接任務就打").isEqualTo(409);
        Res accepted = post("/api/quests/2/accept", null, a.accessToken);
        assertThat(accepted.json.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(post("/api/quests/1/accept", null, a.accessToken).status).as("同時接兩個任務").isEqualTo(409);

        Res result = fightUntilOver(a, 2);
        assertThat(result.json.path("victory").asBoolean()).isTrue();
        assertThat(result.json.path("player").path("exp").asInt()).isEqualTo(50);
        assertThat(result.json.path("player").path("money").asInt()).isEqualTo(650);
        assertThat(get("/api/quests/2", null).json.path("status").asText()).as("打完任務釋放").isEqualTo("AVAILABLE");
        assertThat(battle(a, 2, "ATTACK").status).as("結束後不能再打").isEqualTo(409);

        post("/api/quests/1/accept", null, a.accessToken);
        Res second = fightUntilOver(a, 1);
        assertThat(second.json.path("player").path("level").asInt()).as("第二場升級").isEqualTo(2);
        assertThat(second.json.path("player").path("baseAttack").asInt()).isEqualTo(15);

        Res upgraded = post("/api/store/upgrade-weapon", null, a.accessToken);
        assertThat(upgraded.json.path("weapon").path("name").asText()).isEqualTo("初階獵刀(+1)");
        assertThat(upgraded.json.path("weapon").path("attackBonus").asInt()).isEqualTo(20);
    }

    @Test
    void 喝藥水_離開_落敗() throws Exception {
        Account a = newAccountWithPlayer();
        post("/api/store/potion?type=SMALL", null, a.accessToken);

        post("/api/quests/2/accept", null, a.accessToken);
        Res potion = battle(a, 2, "SMALL_POTION");
        assertThat(potion.json.path("player").path("hp").asInt()).as("滿血喝水，藍速龍 8 點減半受 4").isEqualTo(96);
        Res noPotion = battle(a, 2, "SMALL_POTION");
        assertThat(noPotion.json.path("messages").toString()).contains("沒藥水");

        Res left = battle(a, 2, "LEAVE");
        assertThat(left.json.path("left").asBoolean()).isTrue();
        assertThat(get("/api/quests/2", null).json.path("status").asText()).isEqualTo("AVAILABLE");

        post("/api/quests/7/accept", null, a.accessToken);
        Res lost = fightUntilOver(a, 7);
        assertThat(lost.json.path("defeated").asBoolean()).isTrue();
        assertThat(lost.json.path("player").path("hp").asInt()).isEqualTo(30);
        JsonNode quest7 = get("/api/quests/7", null).json;
        assertThat(quest7.path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(quest7.path("monster").path("hp").asInt()).isEqualTo(quest7.path("monster").path("maxHp").asInt());
    }

    @Test
    void 別人不能打或接我正在打的任務() throws Exception {
        Account me = newAccountWithPlayer();
        Account other = newAccountWithPlayer();
        post("/api/quests/5/accept", null, me.accessToken);

        assertThat(battle(other, 5, "ATTACK").status).isEqualTo(409);
        assertThat(post("/api/quests/5/accept", null, other.accessToken).status).isEqualTo(409);
    }

    // ------------------------------------------------------------------ 權限與錯誤輸入

    @Test
    void 權限規則() throws Exception {
        Account a = newAccountWithPlayer();
        assertThat(get("/api/players/me", null).status).as("沒登入").isEqualTo(401);
        assertThat(get("/api/players/me", "abc.def.ghi").status).as("亂填 token").isEqualTo(401);
        assertThat(get("/api/admin/players", a.accessToken).status).as("一般帳號打 admin").isEqualTo(403);
        assertThat(post("/api/admin/quests/5/release", null, a.accessToken).status).isEqualTo(403);
        assertThat(get("/api/quests", null).status).as("任務板公開").isEqualTo(200);
    }

    @Test
    void 錯誤輸入都回JSON格式的400或404() throws Exception {
        Account a = newAccountWithPlayer();
        assertThat(sendRaw("POST", "/api/battles/action", "{\"questId\":1,\"action\":\"DANCE\"}", a.accessToken).status).isEqualTo(400);
        assertThat(sendRaw("POST", "/api/battles/action", "{oops", a.accessToken).status).isEqualTo(400);
        assertThat(post("/api/battles/action", Map.of("action", "ATTACK"), a.accessToken).status).as("缺 questId").isEqualTo(400);
        assertThat(get("/api/quests/abc", null).status).isEqualTo(400);
        assertThat(post("/api/store/potion?type=HUGE", null, a.accessToken).status).isEqualTo(400);
        assertThat(get("/api/quests/9999", null).status).isEqualTo(404);

        Res notFound = get("/api/nope", a.accessToken);
        assertThat(notFound.status).isEqualTo(404);
        assertThat(notFound.json.has("error")).as("錯誤也要是 {\"error\": ...} 格式").isTrue();
    }

    // ------------------------------------------------------------------ 併發

    @Test
    void 兩個玩家同時接同一個任務只會有一個成功() throws Exception {
        Account a = newAccountWithPlayer();
        Account b = newAccountWithPlayer();

        for (int round = 0; round < 10; round++) {
            List<Integer> statuses = List.of(a, b).parallelStream()
                    .map(acc -> unchecked(() -> post("/api/quests/6/accept", null, acc.accessToken).status))
                    .toList();
            assertThat(statuses).as("第 %d 輪", round).containsExactlyInAnyOrder(200, 409);
            battle(a, 6, "LEAVE");
            battle(b, 6, "LEAVE");
        }
    }

    @Test
    void 連點購買_回200的次數跟實際扣款一致() throws Exception {
        Account a = newAccountWithPlayer();
        JsonNode before = get("/api/players/me", a.accessToken).json;

        List<CompletableFuture<Integer>> clicks = new ArrayList<>();
        IntStream.range(0, 6).forEach(i -> clicks.add(CompletableFuture.supplyAsync(
                () -> unchecked(() -> post("/api/store/potion?type=SMALL", null, a.accessToken).status))));
        List<Integer> statuses = clicks.stream().map(CompletableFuture::join).toList();

        JsonNode after = get("/api/players/me", a.accessToken).json;
        long ok = statuses.stream().filter(s -> s == 200).count();
        assertThat(statuses).allMatch(s -> s == 200 || s == 409);
        assertThat(before.path("money").asInt() - after.path("money").asInt()).isEqualTo(ok * 100);
        assertThat(after.path("smallPotions").asInt() - before.path("smallPotions").asInt()).isEqualTo(ok);
    }

    // ------------------------------------------------------------------ 小工具

    private Account newAccount() throws Exception {
        String name = uniqueName();
        String password = "password-" + UUID.randomUUID();
        post("/api/auth/register", Map.of("username", name, "email", name + "@example.test", "password", password), null);
        JsonNode login = post("/api/auth/login", Map.of("username", name, "password", password), null).json;
        return new Account(name, password, login.path("accessToken").asText(), login.path("refreshToken").asText());
    }

    private Account newAccountWithPlayer() throws Exception {
        Account a = newAccount();
        post("/api/players/me", Map.of("name", "獵人" + a.username.substring(0, 6)), a.accessToken);
        return a;
    }

    private Res battle(Account a, long questId, String action) throws Exception {
        return post("/api/battles/action", Map.of("questId", questId, "action", action), a.accessToken);
    }

    private Res fightUntilOver(Account a, long questId) throws Exception {
        for (int i = 0; i < 30; i++) {
            Res r = battle(a, questId, "ATTACK");
            if (r.status != 200 || r.json.path("battleOver").asBoolean()) {
                return r;
            }
        }
        throw new AssertionError("30 回合內戰鬥沒有結束");
    }

    private static String uniqueName() {
        return "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Res get(String path, String token) throws Exception {
        return sendRaw("GET", path, null, token);
    }

    private Res post(String path, Object body, String token) throws Exception {
        return send("POST", path, body, token);
    }

    private Res send(String method, String path, Object body, String token) throws Exception {
        return sendRaw(method, path, body == null ? null : JSON.writeValueAsString(body), token);
    }

    private Res sendRaw(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json;
        try {
            json = response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
        } catch (Exception notJson) {
            json = JSON.getNodeFactory().textNode(response.body());
        }
        return new Res(response.statusCode(), json);
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T unchecked(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

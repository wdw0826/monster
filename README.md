# MonsterHunter

一個用 **Spring Boot 4 + Spring Security + JWT + Spring Data JPA** 寫的文字版狩獵 RPG 後端。註冊帳號、登入拿 JWT、建立自己的獵人角色，接任務、跟魔物回合制戰鬥、逛商店買藥水／強化武器。所有遊戲操作都綁在登入帳號自己的獵人身上，不能用別人的角色，也不會有兩個玩家同時打同一隻魔物互相干擾血量的問題。

技術棧：**Java 24 · Spring Boot 4.0.6 · Spring Security 7 · PostgreSQL · Flyway · jjwt · Lombok**

## 系統架構

```mermaid
flowchart LR
    Client(["Client\n(curl / Postman)"]) -->|"Authorization: Bearer <JWT>"| Filter[JwtAuthenticationFilter]
    Filter --> Controller
    Controller --> Service
    Service --> Repository
    Repository --> DB[("PostgreSQL")]
    DB --> Repository --> Service --> Controller --> Client
```

## 資料模型

```mermaid
erDiagram
    USERS ||--o| PLAYER : owns
    USERS }o--o{ ROLES : has
    ROLES }o--o{ PERMISSIONS : has
    USERS ||--o{ REFRESH_TOKENS : has
    PLAYER ||--o| WEAPONS : equips
    QUESTS ||--o| MONSTERS : contains
    QUESTS }o--o| PLAYER : "active_player_id"

    USERS {
        Long id
        String username
        String email
        String password
        boolean enabled
    }
    PLAYER {
        Long id
        Long userId
        String name
        int hp
        int maxHp
        int attack
        int level
        int exp
        int money
    }
    WEAPONS {
        Long id
        String name
        int attackBonus
    }
    QUESTS {
        Long id
        String name
        String rank
        String status
        boolean unlocked
        Long activePlayerId
    }
    MONSTERS {
        Long id
        String name
        int hp
        int maxHp
        int attack
        int expValue
    }
```

## 專案結構

```
src/main/java/com/example/monsterhunter/
├── entity/          User, Role, Permission, RefreshToken（帳號） + Player, Weapon, Monster, Quest（遊戲）
├── entity/enums/     QuestRank, QuestStatus
├── security/        JwtUtils, JwtAuthenticationFilter, UserPrincipal, UserDetailsServiceImpl
├── config/          SecurityConfig（API 權限規則）
├── repository/      Spring Data JPA repositories
├── service/         RefreshTokenService（認證） + PlayerService, QuestService, BattleService, StoreService（遊戲）
├── controller/      AuthController（認證） + PlayerController, QuestController, BattleController, StoreController（遊戲）
├── dto/             API 請求/回應物件
└── exception/       自訂例外 + 全域錯誤處理

src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__auth_schema.sql        帳號 / 角色 / 權限 / refresh token
    └── V2__create_game_schema.sql 玩家 / 武器 / 魔物 / 任務 + 任務板種子資料
```

## 如何執行

需要 **Java 24**，以及一個能連到的 **PostgreSQL**——用本機安裝的，或用 Docker 起一個都可以。

### 選項 A：本機已安裝 PostgreSQL（預設抓 `localhost:5432`）

```bash
# 先建立資料庫
psql -U postgres -c "CREATE DATABASE monsterhunter_db;"

# 密碼一定要用環境變數帶入，不要寫進任何檔案
export DB_PASSWORD=你的PostgreSQL密碼
./mvnw spring-boot:run
```

### 選項 B：用 Docker 起一個 PostgreSQL（不想動本機環境的話）

```bash
docker run -d --name my_postgres \
  -e POSTGRES_PASSWORD=my_secret_password \
  -p 5433:5432 postgres:15

docker exec my_postgres psql -U postgres -c "CREATE DATABASE monsterhunter_db;"

export DB_URL=jdbc:postgresql://localhost:5433/monsterhunter_db
export DB_USERNAME=postgres
export DB_PASSWORD=my_secret_password
./mvnw spring-boot:run
```

用 5433 而不是 5432，是為了不跟本機可能已經在跑的 PostgreSQL 搶 port，兩者可以同時並存。

看到 `Started MonsterHunterApplication` 就成功了。Flyway 會自動照 `V1`、`V2` 建好所有表，並種好 9 個任務（1★/2★/3★ 各 3 隻魔物）。

其他可覆蓋的環境變數（都有預設值，本機測試可以不設）：

| 環境變數 | 預設值 | 用途 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/monsterhunter_db` | 資料庫連線位址 |
| `DB_USERNAME` | `postgres` | 資料庫帳號 |
| `DB_PASSWORD` | 無，必填 | 資料庫密碼 |
| `JWT_SECRET` | 內建開發用預設值 | JWT 簽章密鑰，正式環境務必換掉 |
| `SERVER_PORT` | `8080` | 服務監聽 port |

## 認證流程

```mermaid
sequenceDiagram
    participant C as 玩家
    participant API as REST API
    participant DB as 資料庫

    C->>API: POST /api/auth/register {username, email, password}
    API->>DB: 建立帳號（密碼 BCrypt 雜湊）
    API-->>C: 201 註冊成功

    C->>API: POST /api/auth/login {username, password}
    API->>DB: 驗證密碼
    API-->>C: 200 accessToken(15分鐘) + refreshToken(7天)

    C->>API: 帶 Authorization: Bearer <accessToken> 呼叫其他 API
    API-->>C: 通過驗證，回傳資料

    Note over C,API: accessToken 過期後
    C->>API: POST /api/auth/refresh {refreshToken}
    API-->>C: 新的 accessToken + refreshToken（舊的失效）
```

## API 一覽

| 功能 | 需要登入 | Method | Path |
|---|---|---|---|
| 註冊 | 否 | POST | `/api/auth/register` `{"username","email","password"}` |
| 登入 | 否 | POST | `/api/auth/login` `{"username","password"}` |
| 換新 token | 否（要帶 refreshToken） | POST | `/api/auth/refresh` `{"refreshToken"}` |
| 登出 | 否（要帶 refreshToken） | POST | `/api/auth/logout` `{"refreshToken"}` |
| 建立我的獵人 | 是 | POST | `/api/players/me` `{"name":"獵人名稱"}` |
| 查我的獵人狀態 | 是 | GET | `/api/players/me` |
| 任務板 | 否 | GET | `/api/quests` |
| 任務詳情 | 否 | GET | `/api/quests/{id}` |
| 接任務 | 是 | POST | `/api/quests/{id}/accept` |
| 戰鬥（一次呼叫 = 一回合，重複呼叫直到 battleOver=true） | 是 | POST | `/api/battles/action` `{"questId":1,"action":"ATTACK"}` |
| 商店買藥水 | 是 | POST | `/api/store/potion?type=SMALL` 或 `BIG` |
| 商店強化武器 | 是 | POST | `/api/store/upgrade-weapon` |
| 管理端：查看所有玩家的獵人 | 是（需 `ROLE_ADMIN`） | GET | `/api/admin/players` |

`action` 可用值：`ATTACK`（攻擊）、`SMALL_POTION`（喝小藥水）、`BIG_POTION`（喝大藥水）、`LEAVE`（離開戰鬥）。

`/api/admin/**` 這組路徑除了要登入，帳號還要有 `ROLE_ADMIN` 角色，一般使用者（`ROLE_USER`）呼叫會拿到 403，不是 401 也不是 500。

需要登入的端點都要在 header 帶 `Authorization: Bearer <accessToken>`；玩家身分完全從 token 解出來，request 裡不用也不能指定要操作誰的角色。

## 任務戰鬥流程

一個任務同時間只能被一個玩家進行，接任務時會把玩家的 `playerId` 記在任務的 `activePlayerId` 上，戰鬥結束（勝利/失敗/離開）前，其他玩家不能接同一個任務，也不能對它發動戰鬥動作。

```mermaid
sequenceDiagram
    participant P as 玩家（帶 JWT）
    participant API as REST API
    participant DB as 資料庫

    P->>API: POST /api/quests/{id}/accept
    API->>DB: 任務狀態需為 AVAILABLE
    API->>DB: 更新為 IN_PROGRESS，activePlayerId=我的 playerId
    API-->>P: 接受成功

    loop 每回合
        P->>API: POST /battles/action {questId, action}
        API->>API: 驗證 activePlayerId 是不是我，結算本回合傷害／回血
        API-->>P: battleOver=false + 戰鬥紀錄
    end

    P->>API: POST /battles/action（最後一擊）
    API->>DB: 任務重新開放（activePlayerId 清空）、結算經驗/金錢
    API-->>P: battleOver=true, victory/defeated
```

## 如何遊玩（API 呼叫範例）

沒有做前端畫面，遊玩方式是依序呼叫上面的 API（curl、Postman、APIfox 都可以）：

**1. 註冊 + 登入**
```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d "{\"username\":\"hunter1\",\"email\":\"hunter1@example.com\",\"password\":\"password123\"}"

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d "{\"username\":\"hunter1\",\"password\":\"password123\"}"
```
把回傳的 `accessToken` 記下來，之後的呼叫都要帶（以下用 `<TOKEN>` 表示）。

**2. 建立獵人**
```bash
curl -X POST http://localhost:8080/api/players/me -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" -d "{\"name\":\"小明\"}"
```

**3. 看任務板**
```bash
curl http://localhost:8080/api/quests
```
記下想打的任務 `"id"`（以下範例假設是 1）。

**4. 接任務**
```bash
curl -X POST http://localhost:8080/api/quests/1/accept -H "Authorization: Bearer <TOKEN>"
```

**5. 戰鬥（重複呼叫，一次等於一回合）**
```bash
curl -X POST http://localhost:8080/api/battles/action -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" -d "{\"questId\":1,\"action\":\"ATTACK\"}"
```
`action` 可換成 `"SMALL_POTION"`、`"BIG_POTION"`、`"LEAVE"`。`battleOver=true` 代表這輪戰鬥結束。

**6. 逛商店**
```bash
curl -X POST "http://localhost:8080/api/store/potion?type=SMALL" -H "Authorization: Bearer <TOKEN>"
curl -X POST http://localhost:8080/api/store/upgrade-weapon -H "Authorization: Bearer <TOKEN>"
```

**7. 查角色狀態**
```bash
curl http://localhost:8080/api/players/me -H "Authorization: Bearer <TOKEN>"
```

## 怎麼弄一個 ADMIN 帳號

註冊 API 一律只給 `ROLE_USER`，`ROLE_ADMIN` 要手動綁定，不會有人能透過 API 自己把自己升級成管理員：

```bash
# 1. 先照正常流程註冊一個帳號
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d "{\"username\":\"你的帳號\",\"email\":\"你的信箱\",\"password\":\"你的密碼\"}"

# 2. 進資料庫把 ROLE_ADMIN 綁給它（psql 或 DataGrip 都可以）
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = '你的帳號' AND r.name = 'ROLE_ADMIN';
```

綁完要**重新登入**拿新的 token（角色是寫進 JWT 裡的，舊 token 不會自動更新）。之後帶新 token 打 `/api/admin/players` 就能看到所有玩家的獵人角色列表。

## 遊戲數值規則

- 獵人：初始 HP 100、攻擊 10、金錢 500、大藥水 3 瓶，配備「初階獵刀」(+5 攻擊)。
- 升級：exp 每滿 100 升 1 級，攻擊 +5、HP 回滿。
- 戰鬥：攻擊造成「基礎攻擊+武器加成」的傷害；喝藥水本回合不能攻擊，但魔物的反擊傷害減半。
- 狩獵成功：任務重新開放、魔物血量重生、玩家獲得 50 exp、150 金錢，並回復目前損血量的 50%。
- 狩獵失敗（HP 歸零）：任務重新開放、HP 回復至 30。
- 商店：小藥水 $100、大藥水 $200、強化武器 $500（每次攻擊 +15，名稱疊加 `(+1)`、`(+2)`...）。

## 安全性設計

- 密碼一律 BCrypt 雜湊儲存，資料庫密碼與 JWT 簽章密鑰都透過環境變數帶入，不寫死在程式碼或設定檔裡（見上方「如何執行」的環境變數表）。
- 除了註冊/登入/任務瀏覽，其餘 API 一律要求 JWT 驗證（`SecurityConfig` 預設 `anyRequest().authenticated()`）。
- 遊戲角色操作（建立獵人、接任務、戰鬥、商店）都是從登入的 JWT 反解出 `userId` 找到對應的獵人，request 裡不接受呼叫端指定要操作哪個玩家 id，避免冒用他人角色。
- 任務用 `activePlayerId` 鎖定進行中的獵人，避免兩個帳號同時對同一隻魔物發動戰鬥、血量互相干擾。
- 角色分級：一般帳號是 `ROLE_USER`，只能操作自己的獵人；`/api/admin/**` 底下的管理端點要求 `ROLE_ADMIN`，沒有這個角色一律 403（見上方「怎麼弄一個 ADMIN 帳號」）。

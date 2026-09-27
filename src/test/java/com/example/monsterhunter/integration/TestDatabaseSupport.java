package com.example.monsterhunter.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 整合測試共用：每個測試開始前把任務板重設成初始狀態（全部可接、魔物滿血）、清掉任務板快取，
 * 讓每個測試彼此獨立，不會因為上一個測試留下「進行中」的任務而互相影響。
 *
 * 動資料之前會先確認連到的是 monsterhunter_test，避免設定寫錯時誤改到開發用的資料庫。
 */
abstract class TestDatabaseSupport {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetGameState() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog())
                    .as("整合測試只能連 monsterhunter_test，請檢查 application-test.yaml")
                    .isEqualTo("monsterhunter_test");
        }
        jdbc.update("UPDATE quests SET status = 'AVAILABLE', active_player_id = NULL");
        jdbc.update("UPDATE monsters SET hp = max_hp");
        Cache board = cacheManager.getCache("questBoard");
        if (board != null) {
            board.clear();
        }
    }
}

package com.example.monsterhunter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 的基本資訊 + JWT Bearer 認證方案。
 * 有這個 scheme，Swagger UI 右上角才會有「Authorize」按鈕可以貼 access token，
 * 不然像接任務、戰鬥這種要登入的 API，在文件頁上根本測不了。
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI monsterHunterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MonsterHunter API")
                        .description("魔物獵人 REST API：JWT 登入驗證 + 任務/戰鬥/商店玩法")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

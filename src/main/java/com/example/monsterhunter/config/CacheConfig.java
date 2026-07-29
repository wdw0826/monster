package com.example.monsterhunter.config;

import com.example.monsterhunter.dto.QuestResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.List;

/**
 * value 用 JSON 存，不用 JDK 內建序列化：DTO 不用特地實作 Serializable，
 * 而且直接用 redis-cli 讀出來的內容看得懂，方便 demo。
 * 統一設 5 分鐘 TTL 當保險——就算哪個寫入路徑漏掉 @CacheEvict，也不會一直卡著舊資料。
 *
 * questBoard 這個快取單獨用「指定明確型別」的 Jackson2JsonRedisSerializer，不是共用的
 * GenericJackson2JsonRedisSerializer：實測發現 GenericJackson2JsonRedisSerializer 對「頂層是
 * List」的回傳值（getAvailableQuests() 回傳 List&lt;QuestResponse&gt;）多型類型資訊處理不完整，
 * 寫進去沒事，但讀回來會噴 MismatchedInputException("Unexpected token (START_OBJECT), expected
 * VALUE_STRING")。指定明確型別後序列化/反序列化都固定成 List&lt;QuestResponse&gt;，不用猜型別，
 * 這個問題就不會發生。之後如果加新的 @Cacheable 快取，記得比照這裡另外指定型別，
 * 不要直接假設共用的 GenericJackson2JsonRedisSerializer 對所有回傳型別都沒問題。
 *
 * 這裡的 ObjectMapper 是自己 new 的一個，不是注入 Spring 那個共用 bean：
 * 實測發現這個 customizer bean 建立的時機比 JacksonAutoConfiguration 註冊 ObjectMapper bean
 * 還早，注入會直接噴「required a bean of type ObjectMapper that could not be found」，
 * 整個 context 啟動失敗。快取序列化用的 ObjectMapper 本來就跟對外 HTTP 回應那份互相獨立
 * 也比較乾淨，不用特別去搶那個共用 bean。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final String QUEST_BOARD_CACHE = "questBoard";

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper objectMapper = new ObjectMapper();
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        JavaType questListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, QuestResponse.class);
        Jackson2JsonRedisSerializer<List<QuestResponse>> questBoardSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, questListType);
        RedisCacheConfiguration questBoardConfig = defaultConfig
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(questBoardSerializer));

        return builder -> builder
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(QUEST_BOARD_CACHE, questBoardConfig);
    }
}

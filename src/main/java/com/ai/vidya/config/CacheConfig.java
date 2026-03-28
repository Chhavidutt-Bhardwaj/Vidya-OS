package com.ai.vidya.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    // ── Cache name constants ────────────────────────────────────────────────
    public static final String CACHE_SCHOOL          = "school";
    public static final String CACHE_CHAIN           = "chain";
    public static final String CACHE_CHAIN_BRANCHES  = "chainBranches";
    public static final String CACHE_USER_DETAILS    = "userDetails";
    public static final String CACHE_ROLES           = "roles";
    public static final String CACHE_SETTINGS        = "schoolSettings";
    public static final String CACHE_ACADEMIC_YEAR   = "currentAcademicYear";
    public static final String CACHE_DASHBOARD       = "dashboard";
    public static final String STAFF_DETAIL          = "staffDetail";
    public static final String STAFF_LIST            = "staffList";
    public static final String STAFF_PERFORMANCE     = "staffPerformance";
    public static final String AI_INSIGHTS           = "aiInsights";
    public static final String TOP_PERFORMERS        = "topPerformers";

    @Bean
    public ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
        perCacheConfig.put(CACHE_SCHOOL,         defaultConfig.entryTtl(Duration.ofHours(6)));
        perCacheConfig.put(CACHE_CHAIN,          defaultConfig.entryTtl(Duration.ofHours(6)));
        perCacheConfig.put(CACHE_CHAIN_BRANCHES, defaultConfig.entryTtl(Duration.ofHours(2)));
        perCacheConfig.put(CACHE_USER_DETAILS,   defaultConfig.entryTtl(Duration.ofMinutes(60)));
        perCacheConfig.put(CACHE_ROLES,          defaultConfig.entryTtl(Duration.ofHours(12)));
        perCacheConfig.put(CACHE_SETTINGS,       defaultConfig.entryTtl(Duration.ofHours(6)));
        perCacheConfig.put(CACHE_ACADEMIC_YEAR,  defaultConfig.entryTtl(Duration.ofHours(24)));
        perCacheConfig.put(CACHE_DASHBOARD,      defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheConfig.put(STAFF_DETAIL,         defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put(STAFF_LIST,           defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheConfig.put(STAFF_PERFORMANCE,    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        perCacheConfig.put(AI_INSIGHTS,          defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheConfig.put(TOP_PERFORMERS,       defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(perCacheConfig)
            .build();
    }

    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
            CACHE_ROLES, CACHE_SETTINGS, CACHE_ACADEMIC_YEAR
        );
        manager.setCaffeine(
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
        );
        return manager;
    }
}

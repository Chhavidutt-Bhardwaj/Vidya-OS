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

    // ── Cache name constants — use only these strings across the codebase ──

    public static final String CACHE_SCHOOL         = "school";
    public static final String CACHE_CHAIN          = "chain";
    public static final String CACHE_CHAIN_BRANCHES = "chainBranches";
    public static final String CACHE_USER_DETAILS   = "userDetails";
    public static final String CACHE_ROLES          = "roles";
    public static final String CACHE_SETTINGS       = "schoolSettings";
    public static final String CACHE_ACADEMIC_YEAR  = "currentAcademicYear";
    public static final String CACHE_DASHBOARD      = "dashboard";

    // ── Redis serialiser ──────────────────────────────────────────────────

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

    // ── L2: Redis (distributed, survives restart) ─────────────────────────

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();

        // School & chain data — infrequently updated, long TTL
        perCacheConfig.put(CACHE_SCHOOL,
            defaultConfig.entryTtl(Duration.ofHours(6)));
        perCacheConfig.put(CACHE_CHAIN,
            defaultConfig.entryTtl(Duration.ofHours(6)));
        perCacheConfig.put(CACHE_CHAIN_BRANCHES,
            defaultConfig.entryTtl(Duration.ofHours(2)));

        // User details — moderate TTL (role changes must evict)
        perCacheConfig.put(CACHE_USER_DETAILS,
            defaultConfig.entryTtl(Duration.ofMinutes(60)));

        // Roles — long TTL (rarely change)
        perCacheConfig.put(CACHE_ROLES,
            defaultConfig.entryTtl(Duration.ofHours(12)));

        // Settings — long TTL (school settings rarely change)
        perCacheConfig.put(CACHE_SETTINGS,
            defaultConfig.entryTtl(Duration.ofHours(6)));

        // Current academic year — very long (changes once a year)
        perCacheConfig.put(CACHE_ACADEMIC_YEAR,
            defaultConfig.entryTtl(Duration.ofHours(24)));

        // Dashboard — short TTL (frequently changing numbers)
        perCacheConfig.put(CACHE_DASHBOARD,
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(perCacheConfig)
            .build();
    }

    // ── L1: Caffeine (in-process, sub-millisecond for hottest data) ───────

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
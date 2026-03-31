package com.crm.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis connection and cache manager configuration.
 * Dashboard caches expire after 5 minutes (user-scoped keys).
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("dashboard",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("conversionReport",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("salesByRep",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("monthlyTrend",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("activitySummary",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
